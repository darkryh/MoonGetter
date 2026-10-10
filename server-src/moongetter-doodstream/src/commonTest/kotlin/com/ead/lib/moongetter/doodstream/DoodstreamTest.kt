package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DoodstreamTest {
    private val target = "https://dsvplay.com/e/j0jlmfjfalnj"
    private val html = """dsplayer.hotkeys();
        $.get('/pass_md5/abc/def', function(data) { play(data + makePlay()); });
        function makePlay() {
          return random + "?token=abc&expiry=" + Date.now();
        }
    """

    @Test fun constructsMediaWithTwoRequestsAndRequestHeaders() = runBlocking {
        val requests = mutableListOf<Request<*>>()
        val client = object : MoonClient {
            override fun initConfigurationData(configData: Configuration.Data) {}
            override suspend fun <T> request(request: Request<T>): Response {
                requests += request
                return if (requests.size == 1) response(target, html)
                else response(request.url, "https://cdn.example/video/")
            }
        }
        val video = Doodstream(target, client, hashMapOf("User-Agent" to "TestAgent"), Configuration.Data()).onExtract().single()
        assertEquals(2, requests.size)
        assertEquals("https://dsvplay.com/pass_md5/abc/def", requests[1].url)
        assertEquals("https://dsvplay.com", requests[0].headers["Origin"])
        assertEquals(target, requests[1].headers["Referer"])
        assertEquals(target, video.request.headers?.get("Referer"))
        assertEquals("TestAgent", video.request.headers?.get("User-Agent"))
        assertFalse(video.request.headers.orEmpty().containsKey("Set-Cookie"))
        assertTrue(Regex("""https://cdn\.example/video/[A-Za-z0-9]{10}\?token=abc&expiry=\d+""").matches(video.request.url))
        assertTrue(video.request.url.substringAfter("expiry=").toLong() > 1_000_000_000_000)
    }

    @Test fun extractsAfterCanonicalFallbackAndUsesRedirectedHeaders() = runBlocking {
        val requests = mutableListOf<Request<*>>()
        val current = "https://playmogo.com/e/j0jlmfjfalnj"
        val client = object : MoonClient {
            override fun initConfigurationData(configData: Configuration.Data) {}
            override suspend fun <T> request(request: Request<T>): Response {
                requests += request
                return when (requests.size) {
                    1 -> throw IOException("connection closed")
                    2 -> response(current, html)
                    else -> response(request.url, "https://cdn.example/video/")
                }
            }
        }
        val video = Doodstream(target, client, hashMapOf(), Configuration.Data()).onExtract().single()
        assertEquals(3, requests.size)
        assertEquals("https://doodstream.com/e/j0jlmfjfalnj", requests[1].url)
        assertEquals("https://playmogo.com/pass_md5/abc/def", requests[2].url)
        assertEquals(current, requests[2].headers["Referer"])
        assertEquals("https://playmogo.com", requests[2].headers["Origin"])
        assertEquals(current, video.request.headers?.get("Referer"))
    }

    @Test fun convertsClosedConnectionToHandledExtractionError() = runBlocking {
        val error = assertFailsWith<InvalidServerException> {
            Doodstream(target, failingClient(IOException("connection closed")), hashMapOf(), Configuration.Data()).onExtract()
        }
        assertEquals(Error.EMPTY_OR_NULL_RESPONSE, error.error)
        assertTrue(error.message.orEmpty().contains("TLS handshake"))
    }

    @Test fun preservesCancellation() = runBlocking {
        assertFailsWith<CancellationException> {
            Doodstream(target, failingClient(CancellationException("cancelled")), hashMapOf(), Configuration.Data()).onExtract()
        }
        Unit
    }

    @Test fun parsesMultilinePlayerAndExplicitSeconds() {
        assertEquals("/pass_md5/abc/def", DoodstreamPage.endpoint(html))
        assertEquals("?token=abc&expiry=", DoodstreamPage.suffix(html))
        assertFalse(DoodstreamPage.usesSeconds(html))
        assertTrue(DoodstreamPage.usesSeconds(html.replace("Date.now()", "Date.now() / 1000")))
    }

    private fun failingClient(error: Exception) = object : MoonClient {
        override fun initConfigurationData(configData: Configuration.Data) {}
        override suspend fun <T> request(request: Request<T>): Response = throw error
    }

    private fun response(target: String, text: String) = object : Response {
        override val statusCode = 200
        override val headers = mapOf("Set-Cookie" to "test=value")
        override val body = object : ResponseBody { override fun asString() = text }
        override val url = object : Url {
            override val toString = target
            override val host = "dsvplay.com"
        }
    }
}
