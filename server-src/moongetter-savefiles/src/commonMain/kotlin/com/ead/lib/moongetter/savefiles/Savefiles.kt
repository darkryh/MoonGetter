@file:Suppress("RestrictedApi", "VisibleForTests")

package com.ead.lib.moongetter.savefiles

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.js.unpacker.JsUnpacker
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PlaylistUtils
import io.ktor.http.Url

/** Submits the placeholder's embed form without running JavaScript or opening a WebView. */
class Savefiles(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {

    override val headers = headers.also {
        it["Origin"] = Url(url).let { parsed -> "${parsed.protocol.name}://${parsed.host}" }
        it["Referer"] = url
    }

    override suspend fun onExtract(): List<Video> {
        val parsed = Url(url)
        val code = parsed.segments.lastOrNull()?.takeIf { it.matches(Regex("[A-Za-z0-9]+")) }
            ?: throw InvalidServerException("Invalid Savefiles embed code", Error.INVALID_BUILDER_PARAMETERS)
        val origin = "${parsed.protocol.name}://${parsed.host}"
        val response = client.POST(
            requestUrl = "$origin/dl",
            overrideHeaders = mapOf("Accept" to "text/html", "Content-Type" to "application/x-www-form-urlencoded"),
            body = mapOf("op" to "embed", "file_code" to code, "auto" to "1", "referer" to ""),
            asFormUrlEncoded = true
        )
        val html = response.body.asString()
        if (SavefilesPage.isChallenge(html)) throw InvalidServerException(
            "Savefiles returned a Cloudflare verification page instead of the player", Error.EXPECTED_RESPONSE_NOT_FOUND
        )
        if (!response.isSuccess) throw InvalidServerException(
            "Savefiles embed form returned HTTP ${response.statusCode}", Error.UNSUCCESSFUL_RESPONSE, response.statusCode
        )
        val source = SavefilesPage.source(html) ?: throw InvalidServerException(
            "Savefiles player response has no supported video source", Error.EXPECTED_RESPONSE_NOT_FOUND
        )
        val mediaHeaders = headers + ("Referer" to response.url.toString)
        return if (Url(source).encodedPath.endsWith(".m3u8", ignoreCase = true))
            PlaylistUtils(client, mediaHeaders).extractFromHls(source)
        else listOf(Video(url = source, headers = mediaHeaders))
    }
}

internal object SavefilesPage {
    fun source(html: String): String? {
        val text = html + "\n" + (JsUnpacker.unpackAndCombine(html) ?: "")
        val sources = Regex("""\bsources\s*:\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.get(1) ?: return null
        return Regex("""\bfile\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .findAll(sources).map { it.groupValues[1].replace("\\/", "/").replace("&amp;", "&") }
            .firstOrNull { value ->
                value.startsWith("https://") && runCatching {
                    Url(value).encodedPath.let { it.endsWith(".m3u8", true) || it.endsWith(".mp4", true) }
                }.getOrDefault(false)
            }
    }

    fun isChallenge(html: String) = html.contains("/cdn-cgi/challenge-platform/") &&
        html.contains("Just a moment", ignoreCase = true)
}
