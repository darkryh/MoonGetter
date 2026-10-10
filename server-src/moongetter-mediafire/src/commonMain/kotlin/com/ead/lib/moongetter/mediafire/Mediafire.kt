@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.mediafire

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values.targetUrl
import kotlin.io.encoding.Base64

class Mediafire(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also { it.remove("User-Agent") }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET()

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        val html = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }
        val source = MediafirePage.source(html)
            ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
        return listOf(Video(url = source))
    }
}

internal object MediafirePage {
    fun source(html: String): String? {
        val tags = Regex("""<a\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(html)
        for (tag in tags) {
            fun attribute(name: String): String? = Regex("""\b$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                .find(tag.value)?.groupValues?.get(1)
            if (attribute("id") != "downloadButton" && attribute("aria-label") != "Download file") continue
            val scrambled = attribute("data-scrambled-url")
            val source = if (scrambled != null) {
                try { Base64.decode(scrambled).decodeToString() } catch (_: IllegalArgumentException) { null }
            } else attribute("href")
            val decoded = source?.replace("&amp;", "&") ?: continue
            val parsed = runCatching { io.ktor.http.Url(decoded) }.getOrNull() ?: continue
            if (parsed.protocol.name == "https" && parsed.host.endsWith(".mediafire.com") && !decoded.any { it.isWhitespace() }) return decoded
        }
        return null
    }
}
