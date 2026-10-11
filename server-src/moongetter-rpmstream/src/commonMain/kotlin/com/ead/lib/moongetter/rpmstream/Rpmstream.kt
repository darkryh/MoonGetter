@file:Suppress("RestrictedApi", "VisibleForTests")
package com.ead.lib.moongetter.rpmstream

import com.ead.lib.moongetter.rpmstream.factory.RpmstreamFactory
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*

internal expect fun rpmCryptoProvider(): CryptographyProvider

/** Uses the provider's native HLS endpoint, whose child URLs already carry playback tokens. */
class Rpmstream(url: String, client: MoonClient, headers: HashMap<String, String>, configData: Configuration.Data) :
    Server(url, client, headers, configData) {
    override suspend fun onExtract(): List<Video> {
        try {
            // The resolver already chose this server, possibly for a host identified by the fallback.
            val link = RpmLink.parse(url, requireKnownHost = false)
            val playbackHeaders = headers.filterKeys {
                !it.equals("Referer", true) && !it.equals("Origin", true)
            } + mapOf("Referer" to "${link.origin}/", "Origin" to link.origin)
            val response = client.GET(
                requestUrl = "${link.origin}/api/v1/video?id=${link.id}&w=1920&h=1080&r=",
                overrideHeaders = playbackHeaders
            )
            if (!response.isSuccess) fail("RPMStream API HTTP ${response.statusCode}", response.statusCode)
            val payload = RpmPayload.decrypt(response.body.asString())
            val source = RpmPayload.nativeSource(payload, link)
            val playlist = client.GET(requestUrl = source, overrideHeaders = playbackHeaders)
            if (!playlist.isSuccess) fail("RPMStream playlist HTTP ${playlist.statusCode}", playlist.statusCode)
            val manifest = playlist.body.asString().trimStart('\uFEFF', ' ', '\r', '\n')
            check(manifest.startsWith("#EXTM3U") && manifest.lineSequence().any { it.isNotBlank() && !it.startsWith('#') }) {
                "RPMStream playback response is not HLS"
            }
            return listOf(Video(url = source, headers = playbackHeaders))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: InvalidServerException) {
            throw error
        } catch (_: Exception) {
            fail("RPMStream response could not be decrypted or has no supported native HLS source")
        }
    }

    private fun fail(message: String, code: Int = 0): Nothing =
        throw InvalidServerException(message, Error.EXPECTED_RESPONSE_NOT_FOUND, code)
}

internal data class RpmLink(val origin: String, val id: String) {
    companion object {
        /** Parses an embed URL; with [requireKnownHost] false any host with the fragment-ID shape is accepted. */
        fun parse(value: String, requireKnownHost: Boolean = true): RpmLink {
            val descriptor = RpmstreamFactory.descriptor
            val match = (if (requireKnownHost) descriptor.match(value) else descriptor.matchPath(value))
                ?: error("Invalid RPMStream embed URL")
            return RpmLink("https://${match.url.host}", match.id)
        }
    }
}

internal object RpmPayload {
    // Public player protocol constants from index-B82x0F06.js, HTTPS variant.
    @OptIn(DelicateCryptographyApi::class)
    suspend fun decrypt(value: String): JsonObject {
        val hex = value.trim()
        require(hex.length in 32..2_097_152 && hex.length % 32 == 0)
        require(hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' })
        val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        val key = rpmCryptoProvider().get(AES.CBC).keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, "kiemtienmua911ca".encodeToByteArray())
        val plain = key.cipher().decryptWithIv("1234567890oiuytr".encodeToByteArray(), bytes)
        return Json.parseToJsonElement(plain.decodeToString(throwOnInvalidSequence = true)).jsonObject
    }

    fun nativeSource(payload: JsonObject, link: RpmLink): String {
        val id = payload["swarmId"]?.jsonPrimitive?.content
        require(id == link.id) { "RPMStream video ID mismatch" }
        val player = payload["player"]?.jsonObject ?: error("RPMStream video is not ready")
        val restrictions = player["restrictCountry"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val visitor = payload["visitorCountry"]?.jsonPrimitive?.contentOrNull
        require(visitor == null || !restrictions.contains("\"$visitor\"")) { "RPMStream video unavailable in this country" }
        val source = payload["cfNative"]?.jsonPrimitive?.contentOrNull ?: error("Native HLS is unavailable")
        require(source.none(Char::isWhitespace))
        val parsed = Url(source)
        require(parsed.protocol.name == "https" && parsed.host == Url(link.origin).host && parsed.port == 443)
        require(parsed.user.isNullOrEmpty() && parsed.password.isNullOrEmpty() && parsed.fragment.isEmpty())
        require(parsed.encodedPath.endsWith(".m3u8"))
        require(!parsed.parameters["k"].isNullOrBlank() && !parsed.parameters["kx"].isNullOrBlank())
        return source
    }
}
