@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.filemoon

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.js.unpacker.JsUnpacker
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.PlaylistUtils
import com.ead.lib.moongetter.utils.Values.targetUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom

@ExperimentalServer
class Filemoon(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    override val headers: HashMap<String, String> = headers.also {
        it["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        it["Accept-Language"] = "en-US,en;q=0.5"
        it["Priority"] = "u=0, i"
        it["Origin"] = Url(url).let { "${it.protocol.name}://${it.host}" }
        it["Referer"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        val visited = mutableSetOf<String>()
        repeat(3) {
            if (!visited.add(url)) throw missingPlayer("Filemoon iframe redirect loop")
            val response = client.GET()
            if (!response.isSuccess) throw InvalidServerException(
                Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode
            )
            val html = response.body.asString().ifEmpty {
                throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)
            }
            FilemoonPage.source(html)?.let { source ->
                return playlistUtils.extractFromHls(source, headers + ("Referer" to response.url.toString))
            }
            if (FilemoonPage.isByse(html)) return try {
                extractByse(response.url.toString)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: InvalidServerException) {
                throw error
            } catch (_: Exception) {
                throw missingPlayer("Byse verification or playback response could not be parsed")
            }
            val iframe = FilemoonPage.iframe(html) ?: throw missingPlayer(Resources.expectedResponseNotFound(name))
            headers["Referer"] = response.url.toString
            url = URLBuilder(response.url.toString).takeFrom(iframe).buildString()
        }
        throw missingPlayer("Filemoon iframe depth exceeded")
    }

    private suspend fun extractByse(embedUrl: String): List<Video> {
        val parsed = Url(embedUrl)
        val code = parsed.segments.lastOrNull() ?: throw missingPlayer("Missing Byse video code")
        val endpoint = "${parsed.protocol.name}://${parsed.host}/api/videos/$code/embed"
        val apiHeaders = headers + mapOf("Accept" to "application/json", "Content-Type" to "application/json", "Referer" to embedUrl)
        suspend fun post(path: String, body: JsonObject, token: String? = null): JsonObject {
            val response = client.POST(
                requestUrl = "$endpoint/$path",
                overrideHeaders = apiHeaders + (token?.let { mapOf("X-Captcha-Token" to it) } ?: emptyMap()),
                body = body
            )
            if (!response.isSuccess) throw InvalidServerException(
                "Byse $path returned HTTP ${response.statusCode}", Error.UNSUCCESSFUL_RESPONSE, response.statusCode
            )
            return Json.parseToJsonElement(response.body.asString()).jsonObject
        }
        val challenge = post("captcha", buildJsonObject {})
        val solution = withContext(Dispatchers.Default) {
            withTimeoutOrNull(60_000) {
                ByseProofOfWork.solve(challenge.getValue("pow_nonce").jsonPrimitive.content,
                    challenge.getValue("pow_difficulty").jsonPrimitive.int)
            }
        } ?: throw InvalidServerException("Byse verification work budget exceeded", Error.TIMEOUT_ERROR)
        val verification = post("captcha/verify", buildJsonObject {
            put("pow_token", challenge.getValue("pow_token"))
            put("solution", solution)
        })
        if (verification["status"]?.jsonPrimitive?.content != "ok")
            throw missingPlayer("Byse verification rejected the work solution")
        val token = verification["token"]?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }
            ?: throw missingPlayer("Byse verification did not return a token")
        // The API requires this object even when no browser fingerprint is supplied.
        val playback = post("playback", buildJsonObject {
            put("fingerprint", buildJsonObject {
                put("token", "")
                put("viewer_id", "")
                put("device_id", "")
            })
        }, token)
        val decoded = try {
            BysePlayback.decode(playback.getValue("playback").jsonObject)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw missingPlayer("Byse playback payload could not be authenticated or decoded")
        }
        val mediaHeaders = headers + ("Referer" to embedUrl)
        val sources = decoded["sources"]?.jsonArray ?: throw missingPlayer("Byse playback has no sources")
        val videos = mutableListOf<Video>()
        for (entry in sources) {
            val source = entry.jsonObject
            val mediaUrl = source["url"]?.jsonPrimitive?.content ?: continue
            if (!mediaUrl.startsWith("https://")) continue
            val mime = source["mime_type"]?.jsonPrimitive?.content.orEmpty()
            if (mime.contains("mpegurl", ignoreCase = true) || Url(mediaUrl).encodedPath.endsWith(".m3u8"))
                videos += playlistUtils.extractFromHls(mediaUrl, mediaHeaders)
            else videos += Video(quality = source["label"]?.jsonPrimitive?.content, url = mediaUrl, headers = mediaHeaders)
        }
        return videos.ifEmpty { throw missingPlayer("Byse playback has no usable sources") }
    }

    private fun missingPlayer(message: String) =
        InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND)
}

internal object FilemoonPage {
    fun source(html: String): String? {
        val script = JsUnpacker.unpackAndCombine(html) ?: return null
        return PatternManager.singleMatch(script, """(https://[^\s"']+\.m3u8(?:\?[^\s"']*)?)""")
    }

    fun iframe(html: String): String? = Regex(
        """<iframe\b[^>]*\bsrc\s*=\s*["']((?:https?://|/)[^"']+)["'][^>]*>""",
        RegexOption.IGNORE_CASE
    ).find(html)?.groupValues?.get(1)

    fun isByse(html: String): Boolean = html.contains("Byse Frontend", ignoreCase = true) &&
        html.contains("id=\"root\"")
}
