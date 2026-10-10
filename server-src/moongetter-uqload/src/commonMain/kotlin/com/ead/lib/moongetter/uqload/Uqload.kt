@file:Suppress("RestrictedApi", "VisibleForTests")

package com.ead.lib.moongetter.uqload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.js.unpacker.JsUnpacker
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.http.Url

class Uqload(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {
    override suspend fun onExtract(): List<Video> {
        val input = UqloadPayload.embed(url) ?: fail("Invalid Uqload embed URL")
        val baseHeaders = headers.filterKeys { !it.equals("Referer", true) && !it.equals("Origin", true) }
        fun playbackHeaders(embed: String) = baseHeaders + mapOf(
            "Referer" to embed,
            "Origin" to "https://${Url(embed).host}"
        )
        val response = client.GET(requestUrl = url, overrideHeaders = playbackHeaders(url))
        if (!response.isSuccess) fail("Uqload embed HTTP ${response.statusCode}", response.statusCode)
        val canonical = response.url.toString
        if (UqloadPayload.embed(canonical) != input) fail("Uqload redirected to an unrelated embed")
        val mediaHeaders = playbackHeaders(canonical)
        val sources = UqloadPayload.sources(response.body.asString())
        if (sources.isEmpty()) fail("Uqload embed contains no playable player sources")
        return sources.map { source ->
            if (Url(source).encodedPath.endsWith(".m3u8", true)) {
                val playlist = client.GET(requestUrl = source, overrideHeaders = mediaHeaders)
                if (!playlist.isSuccess) fail("Uqload playlist HTTP ${playlist.statusCode}", playlist.statusCode)
                val manifest = playlist.body.asString().trimStart('\uFEFF', ' ', '\r', '\n')
                if (!manifest.startsWith("#EXTM3U") || manifest.lineSequence().none { it.isNotBlank() && !it.startsWith('#') }) {
                    fail("Uqload source is an HTML placeholder or an empty playlist")
                }
            }
            Video(url = source, headers = mediaHeaders)
        }
    }

    private fun fail(message: String, code: Int = 0): Nothing =
        throw InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND, code)
}

internal object UqloadPayload {
    const val PATTERN = """^https://(?:www\.)?uqload\.[a-z]{2,}/embed-([A-Za-z0-9]+)\.html(?:\?[^\s#]*)?(?:#[^\s]*)?$"""
    fun embed(value: String): String? = Regex(PATTERN).matchEntire(value)?.groupValues?.get(1)

    fun sources(html: String): List<String> {
        val script = JsUnpacker.unpackAndCombine(html) ?: html
        val arrays = Regex("""["']?sources["']?\s*:\s*\[([\s\S]*?)]""", RegexOption.IGNORE_CASE).findAll(script)
        return arrays.flatMap { array ->
            val body = array.groupValues[1]
            val objects = Regex("""["']?file["']?\s*:\s*["']([^"']+)["']""").findAll(body).map { it.groupValues[1] }.toList()
            if (objects.isNotEmpty()) objects.asSequence()
            else Regex("""["']([^"']+)["']""").findAll(body).map { it.groupValues[1] }
        }.map { it.replace("\\/", "/").replace("&amp;", "&") }.filter { value ->
            runCatching {
                val parsed = Url(value)
                parsed.protocol.name == "https" && parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty() &&
                    parsed.host.isNotEmpty() && value.none(Char::isWhitespace) &&
                    (parsed.encodedPath.endsWith(".m3u8", true) || parsed.encodedPath.endsWith(".mp4", true))
            }.getOrDefault(false)
        }.distinct().toList()
    }
}
