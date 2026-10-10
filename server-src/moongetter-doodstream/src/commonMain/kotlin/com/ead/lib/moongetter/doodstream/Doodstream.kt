package com.ead.lib.moongetter.doodstream

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
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class Doodstream(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {

    override val headers = headers.also {
        it["Origin"] = Url(url).let { parsed -> "${parsed.protocol.name}://${parsed.host}" }
        it["Referer"] = url
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun onExtract(): List<Video> = try {
        val page = DoodstreamResolver.resolve(url, headers) { target, requestHeaders ->
            client.GET(requestUrl = target, overrideHeaders = requestHeaders)
        }
        if (!page.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, page.statusCode)
        val html = page.body.asString()
        val path = DoodstreamPage.endpoint(html) ?: throw missing("Doodstream player has no pass_md5 endpoint")
        val suffix = DoodstreamPage.suffix(html) ?: throw missing("Doodstream player has no token suffix")
        val referer = page.url.toString
        val origin = Url(referer).let { "${it.protocol.name}://${it.host}" }
        val mediaHeaders = headers + mapOf("Origin" to origin, "Referer" to referer)
        val endpoint = URLBuilder(referer).takeFrom(path).buildString()
        val sourceResponse = client.GET(requestUrl = endpoint, overrideHeaders = mediaHeaders)
        if (!sourceResponse.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, sourceResponse.statusCode)
        val base = sourceResponse.body.asString().trim()
        if (!base.startsWith("https://") || base.any { it.isWhitespace() }) throw missing("Doodstream pass_md5 response has no media URL")
        val random = List(10) { "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")
        val now = Clock.System.now().toEpochMilliseconds()
        // Follow the explicit seconds conversion when present; makePlay normally uses Date.now().
        val expiry = if (DoodstreamPage.usesSeconds(html)) now / 1000 else now
        listOf(Video(url = base + random + suffix + expiry, headers = mediaHeaders))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        throw InvalidServerException("Doodstream connection failed before a usable response (network or TLS handshake)", Error.EMPTY_OR_NULL_RESPONSE)
    }

    private fun missing(message: String) = InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND)
}

internal object DoodstreamPage {
    fun isChallenge(html: String): Boolean = html.contains("/cdn-cgi/challenge-platform/") &&
        (html.contains("Just a moment", ignoreCase = true) || html.contains("cf-chl-"))

    fun endpoint(html: String): String? = Regex("""["'](/pass_md5/[^"']+)["']""").find(html)?.groupValues?.get(1)
    fun suffix(html: String): String? = Regex(
        """makePlay[\s\S]*?return[\s\S]*?["'](\?token=[^"']+)["']"""
    ).find(html)?.groupValues?.get(1)?.replace("&amp;", "&")

    fun usesSeconds(html: String): Boolean = Regex(
        """(?:Date\.now\(\)|getTime\(\))\s*/\s*1000"""
    ).containsMatchIn(html)
}
