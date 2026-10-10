@file:OptIn(ExperimentalEncodingApi::class)

package com.ead.lib.moongetter.voe

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Voe(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val visited = mutableSetOf<String>()
        repeat(4) {
            if (!visited.add(url)) throw expectedResponseNotFound()
            val response = client.GET()
            if (!response.isSuccess) throw InvalidServerException(
                Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode
            )
            val html = response.body.asString().ifEmpty {
                throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)
            }
            VoeSource.extract(html)?.let { source ->
                return listOf(Video(url = source, headers = headers + ("Referer" to response.url.toString)))
            }
            val redirect = Regex("""window\.location\.href\s*=\s*['"]([^'"]+)['"]""")
                .find(html)?.groupValues?.get(1) ?: throw expectedResponseNotFound()
            url = URLBuilder(response.url.toString).takeFrom(redirect).buildString()
        }
        throw expectedResponseNotFound()
    }

    private fun expectedResponseNotFound() = InvalidServerException(
        Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND
    )
}

/** Decodes the data configuration only; never executes the page's JavaScript. */
internal object VoeSource {
    fun extract(html: String): String? {
        val scripts = Regex("""<script\b([^>]*)>([\s\S]*?)</script\s*>""",
            RegexOption.IGNORE_CASE)
        val jsonType = Regex("""\btype\s*=\s*['"]application/json['"]""", RegexOption.IGNORE_CASE)
        for (script in scripts.findAll(html)) {
            if (!jsonType.containsMatchIn(script.groupValues[1])) continue
            val source = runCatching {
                val payload = Json.parseToJsonElement(script.groupValues[2]) as? JsonArray
                val encoded = (payload?.firstOrNull() as? JsonPrimitive)?.takeIf { it.isString }?.content
                    ?: return@runCatching null
                val config = Json.parseToJsonElement(decode(encoded)) as? JsonObject
                (config?.get("source") as? JsonPrimitive)?.takeIf { it.isString }?.content
            }.getOrNull()
            if (isMediaUrl(source)) return source
        }
        // Older VOE pages store the HLS URL directly or as Base64.
        val legacy = Regex("""['"]hls['"]\s*:\s*['"]([^'"]+)['"]""")
            .find(html)?.groupValues?.get(1) ?: return null
        val source = if (isMediaUrl(legacy)) legacy else runCatching {
            Base64.decode(legacy).decodeToString()
        }.getOrNull()
        return source?.takeIf { isMediaUrl(it) }
    }

    private fun decode(encoded: String): String {
        var value = encoded.map { char ->
            when (char) {
                in 'A'..'Z' -> ('A'.code + (char.code - 'A'.code + 13) % 26).toChar()
                in 'a'..'z' -> ('a'.code + (char.code - 'a'.code + 13) % 26).toChar()
                else -> char
            }
        }.joinToString("")
        for (token in listOf("@$", "^^", "~@", "%?", "*~", "!!", "#&", "_")) {
            value = value.replace(token, "")
        }
        value = Base64.decode(value).decodeToString()
            .map { (it.code - 3).toChar() }.joinToString("").reversed()
        return Base64.decode(value).decodeToString()
    }

    private fun isMediaUrl(value: String?): Boolean = value != null && runCatching {
        val parsed = Url(value)
        parsed.protocol.name in setOf("http", "https") && parsed.host.isNotBlank() &&
            (parsed.encodedPath.endsWith(".m3u8", ignoreCase = true) ||
                parsed.encodedPath.endsWith(".mp4", ignoreCase = true)) &&
            !parsed.host.equals("test-videos.co.uk", ignoreCase = true)
    }.getOrDefault(false)
}
