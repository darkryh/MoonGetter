package com.ead.lib.moongetter.onecloudfile

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.http.Url

class OneCloudFile(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {
    override suspend fun onExtract(): List<Video> {
        if (!Regex(OneCloudPayload.PATTERN).matches(url)) fail("Invalid 1CloudFile URL")
        val page = client.GET(requestUrl = url)
        if (!page.isSuccess) fail("1CloudFile page HTTP ${page.statusCode}")
        val canonical = page.url.toString
        if (!Regex(OneCloudPayload.PATTERN).matches(canonical) || Url(canonical).encodedPath != Url(url).encodedPath) {
            fail("1CloudFile redirected to an unrelated file")
        }
        val html = page.body.asString()
        if (html.contains("Notice ! - 1Cloud File")) fail("1CloudFile resource is unavailable", Error.RESOURCE_TAKEN_DOWN)
        val source = OneCloudPayload.source(html) ?: fail("1CloudFile download encoding is missing or unsupported")
        if (!Url(source).encodedPath.startsWith("${Url(canonical).encodedPath}/")) fail("1CloudFile source belongs to another file")
        val playbackHeaders = headers.filterKeys { !it.equals("Referer", true) && !it.equals("Origin", true) } +
            mapOf("Referer" to canonical, "Origin" to "https://1cloudfile.com")
        // HEAD validates the signed ticket without downloading the whole file when Range is ignored.
        val media = client.request<String>(Request(method = HttpMethod.GET, url = source, headers = playbackHeaders, isResponseBodyNeeded = false))
        if (!media.isSuccess) fail("1CloudFile media HTTP ${media.statusCode}")
        val type = media.headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value.orEmpty().substringBefore(';').trim()
        if (type !in setOf("video/mp4", "video/webm", "video/x-matroska", "application/octet-stream")) {
            fail("1CloudFile download returned an unsupported content type")
        }
        if (!OneCloudPayload.validSource(media.url.toString)) fail("1CloudFile media redirected outside its file servers")
        return listOf(Video(url = source, headers = playbackHeaders))
    }

    private fun fail(message: String, error: Error = Error.EXPECTED_RESPONSE_NOT_FOUND): Nothing =
        throw InvalidServerException(message, error)
}

internal object OneCloudPayload {
    const val PATTERN = """^https://(?:www\.)?1cloudfile\.com/[A-Za-z0-9]+(?:\?[^\s#]*)?$"""

    fun source(html: String): String? {
        // The current page passes a hex-encoded XOR string to its download-link function.
        val match = Regex("""\b[A-Za-z_$][\w$]*\s*\(\s*["']([0-9a-fA-F]+)["']\s*,\s*["']DOWNLOAD FILE\b""").find(html) ?: return null
        val hex = match.groupValues[1]
        if (hex.length % 2 != 0 || hex.length > 16_384) return null
        val decoded = runCatching {
            hex.chunked(2).map { (it.toInt(16) xor 0x7a).toByte() }.toByteArray().decodeToString(throwOnInvalidSequence = true)
        }.getOrNull() ?: return null
        return decoded.takeIf(::validSource)
    }

    fun validSource(value: String): Boolean = runCatching {
        val parsed = Url(value)
        parsed.protocol.name == "https" && Regex("""^fs[0-9]+\.1cloudfile\.com$""").matches(parsed.host) &&
            parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty() && parsed.port == 443 &&
            value.none { it.isWhitespace() || it.code < 32 } &&
            listOf(".mp4", ".webm", ".mkv").any { parsed.encodedPath.endsWith(it, true) }
    }.getOrDefault(false)
}
