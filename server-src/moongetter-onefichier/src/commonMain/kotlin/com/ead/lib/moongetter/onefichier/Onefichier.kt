package com.ead.lib.moongetter.onefichier

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

/** Resolves the public free download form, respecting the provider's waiting time. */
class Onefichier(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {
    override suspend fun onExtract(): List<Video> {
        if (!Regex(OnefichierPage.PATTERN).matches(url)) fail("Invalid 1fichier file URL")
        val page = client.GET(requestUrl = url)
        if (!page.isSuccess) fail("1fichier page HTTP ${page.statusCode}")
        val html = page.body.asString()
        OnefichierPage.restriction(html)?.let { fail(it) }
        val form = OnefichierPage.form(html, url) ?: fail("1fichier returned no supported free download form")
        val name = Regex("""class=["']tier-name["'][^>]*>([^<]+)""").find(html)?.groupValues?.get(1)
            ?: fail("1fichier returned no file metadata")
        if (!listOf(".mp4", ".mkv", ".webm", ".mov", ".m4v").any { name.trim().endsWith(it, true) })
            fail("1fichier file is not a supported video")
        if (form.waitSeconds > 0) delay(form.waitSeconds * 1000L)
        val playbackHeaders = headers.filterKeys { !it.equals("Referer", true) && !it.equals("Origin", true) } +
            mapOf("Referer" to url, "Origin" to "https://1fichier.com")
        val result = client.POST(requestUrl = form.url, overrideHeaders = playbackHeaders,
            body = form.fields + ("dlinline" to "on"), asFormUrlEncoded = true)
        if (!result.isSuccess) fail("1fichier download form HTTP ${result.statusCode}")
        val ticketHtml = result.body.asString()
        OnefichierPage.restriction(ticketHtml)?.let { fail(it) }
        val source = OnefichierPage.source(ticketHtml) ?: fail("1fichier returned no free download source")
        val media = client.request<String>(Request(method = HttpMethod.GET, url = source,
            headers = playbackHeaders, isResponseBodyNeeded = false))
        val type = media.headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value.orEmpty().substringBefore(';').trim().lowercase()
        if (!media.isSuccess || !OnefichierPage.validSource(media.url.toString) ||
            !(type.startsWith("video/") || type == "application/octet-stream"))
            fail("1fichier download source could not be validated as media")
        return listOf(Video(url = media.url.toString, headers = playbackHeaders))
    }
    private fun fail(message: String): Nothing = throw InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND)
}

internal object OnefichierPage {
    const val PATTERN = """^https://(?:www\.)?1fichier\.com/\?[A-Za-z0-9]+(?:&(?:lg=[a-z]{2}))?$"""
    data class Form(val url: String, val waitSeconds: Long, val fields: Map<String, String>)
    private fun attributes(tag: String): Map<String, String> = Regex("""([\w-]+)\s*=\s*["']([^"']*)["']""")
        .findAll(tag).associate { it.groupValues[1].lowercase() to it.groupValues[2].replace("&amp;", "&") }
    fun form(html: String, original: String): Form? {
        val form = Regex("""<form\b([^>]*)>([\s\S]*?)</form>""", RegexOption.IGNORE_CASE)
            .findAll(html).firstOrNull { attributes(it.groupValues[1])["id"] == "f1" } ?: return null
        val attrs = attributes(form.groupValues[1])
        if (!attrs["method"].equals("post", true)) return null
        val target = attrs["action"] ?: return null
        if (!Regex(PATTERN).matches(target) || Url(target).encodedQuery.substringBefore('&') != Url(original).encodedQuery.substringBefore('&')) return null
        val wait = Regex("""\bvar\s+ct\s*=\s*(\d+)\s*;""").find(html)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        if (wait !in 0..60) return null
        val fields = Regex("""<input\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(form.groupValues[2])
            .map { attributes(it.value) }.filter { it["type"].equals("hidden", true) && it["name"] != null }
            .associate { it.getValue("name") to it["value"].orEmpty() }
        return Form(target, wait, fields)
    }
    fun validSource(value: String): Boolean = runCatching {
        val u = Url(value)
        u.protocol.name == "https" && u.port == 443 && u.user == null && u.password == null &&
            u.host.endsWith(".1fichier.com") && u.host != "www.1fichier.com" && u.host != "img.1fichier.com" &&
            u.encodedPath != "/" && value.none { it.isWhitespace() || it.code < 32 }
    }.getOrDefault(false)
    fun source(html: String): String? = Regex("""<a\b[^>]*>""", RegexOption.IGNORE_CASE).findAll(html)
        .map { attributes(it.value) }.firstOrNull { a ->
            a["class"].orEmpty().split(Regex("\\s+")).contains("ok") && a["href"]?.let(::validSource) == true
        }?.get("href")
    fun restriction(html: String): String? = when {
        html.contains("all free guest slots", true) -> "1fichier free guest slots are occupied; try later or use an existing free-account session"
        html.contains("you must wait", true) || html.contains("you have to wait", true) -> "1fichier requires a cooldown before another free download; try later"
        html.contains("name=\"pass\"", true) || html.contains("name='pass'", true) -> "1fichier file requires a password; password-protected files are unsupported"
        html.contains("captcha", true) && (html.contains("g-recaptcha", true) || html.contains("h-captcha", true)) -> "1fichier requires interactive verification"
        html.contains("File not found", true) || html.contains("file has been deleted", true) -> "1fichier file is unavailable"
        else -> null
    }
}
