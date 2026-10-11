package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.doodstream.factory.DoodstreamFactory
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.http.Url
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/** Resolves legacy aliases through the provider's canonical entry, without pinning its rotating mirror. */
internal object DoodstreamResolver {
    suspend fun resolve(target: String, headers: Map<String, String>, fetch: suspend (String, Map<String, String>) -> Response): Response {
        // Path shape only: the resolver may have identified this host through the fallback.
        requireNotNull(DoodstreamFactory.descriptor.matchPath(target)) { "Unsupported Doodstream URL" }
        val input = Url(target)
        val code = input.segments.last()
        val canonical = "https://doodstream.com/e/$code" + input.encodedQuery.takeIf { it.isNotEmpty() }?.let { "?$it" }.orEmpty()
        var failure = InvalidServerException("Doodstream resolution has no usable response", Error.EMPTY_OR_NULL_RESPONSE)
        // At most the supplied address and one authoritative provider entry. The HTTP client follows redirects.
        for (candidate in listOf(target, canonical).distinct()) {
            val parsed = Url(candidate)
            val origin = "${parsed.protocol.name}://${parsed.host}"
            val response = try {
                fetch(candidate, headers + mapOf("Origin" to origin, "Referer" to candidate))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                failure = InvalidServerException("Doodstream connection failed at ${parsed.host} (network or TLS handshake)", Error.EMPTY_OR_NULL_RESPONSE)
                continue
            } catch (error: InvalidServerException) {
                if (error.error != Error.TIMEOUT_ERROR) throw error
                failure = error
                continue
            }
            val final = Url(response.url.toString)
            // A redirect must retain this video, even when the provider rotates its serving hostname.
            if (final.protocol.name != "https" || final.segments != listOf("e", code)) {
                failure = InvalidServerException("Doodstream redirect did not retain the HTTPS embed/video ID", Error.EXPECTED_RESPONSE_NOT_FOUND)
                continue
            }
            val html = response.body.asString()
            failure = when {
                DoodstreamPage.isChallenge(html) -> InvalidServerException(
                    "Doodstream resolved to ${final.host}, but verification is required (HTTP ${response.statusCode}); player HTML was not received",
                    Error.EXPECTED_RESPONSE_NOT_FOUND, response.statusCode
                )
                !response.isSuccess -> InvalidServerException("Doodstream resolved to ${final.host}, but returned HTTP ${response.statusCode}", Error.UNSUCCESSFUL_RESPONSE, response.statusCode)
                DoodstreamPage.endpoint(html) == null || DoodstreamPage.suffix(html) == null -> InvalidServerException(
                    "Doodstream resolved to ${final.host}, but its response contains no supported player configuration", Error.EXPECTED_RESPONSE_NOT_FOUND
                )
                else -> return response
            }
        }
        throw failure
    }
}
