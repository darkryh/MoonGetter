package com.ead.lib.moongetter.fireload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

class Fireload(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {
    override suspend fun onExtract(): List<Video> {
        val id = FireloadPayload.fileId(url) ?: fail("Invalid Fireload file URL")
        val page = client.GET(requestUrl = url)
        if (!page.isSuccess) fail("Fireload page HTTP ${page.statusCode}")
        val canonical = page.url.toString
        if (FireloadPayload.fileId(canonical) != id) fail("Fireload redirected to an unrelated file")
        val html = page.body.asString()
        if (html.contains("Error | Fireload")) fail("Fireload resource is unavailable", Error.RESOURCE_TAKEN_DOWN)
        val ticket = FireloadPayload.ticket(html, id) ?: fail("Fireload page contains no supported download ticket")
        val playbackHeaders = headers.filterKeys { !it.equals("Cookie", true) && !it.equals("Referer", true) && !it.equals("Origin", true) } +
            mapOf("Referer" to canonical, "Origin" to "https://${Url(canonical).host}")
        val cookie = FireloadPayload.cookie(page.headers)
        val ticketHeaders = playbackHeaders + (cookie?.let { mapOf("Cookie" to it) } ?: emptyMap())
        if (ticket.waitSeconds > 0) delay(ticket.waitSeconds * 1000L)
        // KtorMoonClient maps a bodyless GET to HEAD. Resolve the single-use ticket once.
        val media = client.request<String>(Request(method = HttpMethod.GET, url = ticket.url, headers = ticketHeaders, isResponseBodyNeeded = false))
        if (!media.isSuccess) fail("Fireload media HTTP ${media.statusCode}")
        val source = media.url.toString
        if (!FireloadPayload.validMedia(source, id)) fail("Fireload ticket did not resolve to the matching file server")
        val type = media.headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value.orEmpty().substringBefore(';').trim()
        if (type !in setOf("video/mp4", "video/webm", "video/x-matroska", "application/octet-stream")) {
            fail("Fireload ticket returned an HTML placeholder or unsupported media")
        }
        // The resolved server URL was validated without cookies; do not give the player the one-use page ticket.
        return listOf(Video(url = source, headers = playbackHeaders))
    }

    private fun fail(message: String, error: Error = Error.EXPECTED_RESPONSE_NOT_FOUND): Nothing =
        throw InvalidServerException(message, error)
}

internal object FireloadPayload {
    const val PATTERN = """^https://(?:www\.)?fireload\.com/([a-fA-F0-9]{16})(?:/[^\s?#]+)?(?:\?[^\s#]*)?$"""
    data class Ticket(val url: String, val waitSeconds: Long)
    fun fileId(value: String): String? = Regex(PATTERN).matchEntire(value)?.groupValues?.get(1)

    fun ticket(html: String, id: String): Ticket? {
        val start = Regex("""\bwindow\.Fl\s*=\s*\{""").find(html)?.range?.last ?: return null
        var quoted = false
        var escaped = false
        var depth = 0
        var end = -1
        for (i in start until minOf(html.length, start + 16_384)) {
            val c = html[i]
            if (quoted) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') quoted = false
            } else when (c) {
                '"' -> quoted = true
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) { end = i; break } }
            }
        }
        if (end < 0) return null
        val root = runCatching { Json.parseToJsonElement(html.substring(start, end + 1)) as? JsonObject }.getOrNull() ?: return null
        val link = (root["dlink"] as? JsonPrimitive)?.contentOrNull ?: return null
        if (fileId(link) != id) return null
        val parsed = Url(link)
        if (parsed.user != null || parsed.password != null || parsed.port != 443 || parsed.parameters["pt"].isNullOrBlank()) return null
        val wait = (root["dwait"] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: return null
        if (wait !in 0..60) return null
        return Ticket(link, wait)
    }

    fun validMedia(value: String, id: String): Boolean = runCatching {
        val u = Url(value)
        u.protocol.name == "https" && Regex("""^srv[0-9]+\.fireload\.com$""").matches(u.host) &&
            u.port == 443 && u.user.isNullOrEmpty() && u.password.isNullOrEmpty() && value.none { it.isWhitespace() || it.code < 32 } &&
            u.encodedPath.startsWith("/$id/") && listOf(".mp4", ".webm", ".mkv").any { u.encodedPath.endsWith(it, true) }
    }.getOrDefault(false)

    fun cookie(headers: Map<String, String>): String? {
        val value = headers.entries.firstOrNull { it.key.equals("Set-Cookie", true) }?.value ?: return null
        return Regex("""(?:^|[,;]\s*)filehosting=([^;,\s]+)""").find(value)?.groupValues?.get(1)?.let { "filehosting=$it" }
    }
}
