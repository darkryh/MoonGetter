package com.ead.lib.moongetter.voe

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalEncodingApi::class)
class VoeTest {

    private var requestCount = 0
    private val responses = mutableListOf<Pair<String, Int>>()

    private fun createMockClient(): MoonClient {
        return object : MoonClient(Configuration.Data()) {
            override suspend fun GET() = createMockResponse()
        }
    }

    private fun createMockResponse() = object {
        val currentResponse = responses[requestCount++]
        val isSuccess = currentResponse.second in 200..299
        val statusCode = currentResponse.second
        val body = object {
            fun asString() = currentResponse.first
        }
    }

    @Test
    fun testSuccessfulTwoStepExtraction() = runTest {
        requestCount = 0
        responses.clear()

        val redirectHtml = """window.location.href = 'https://voe.com/actual-video'"""
        val videoUrl = "https://example.com/stream.m3u8"
        val encodedUrl = Base64.encode(videoUrl.encodeToByteArray())
        val videoHtml = """'hls': '$encodedUrl'"""

        responses.add(redirectHtml to 200)
        responses.add(videoHtml to 200)

        val client = createMockClient()
        val server = Voe(
            url = "https://voe.com/initial",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertEquals(videoUrl, videos[0].url)
    }

    @Test
    fun testMissingRedirectUrl() = runTest {
        requestCount = 0
        responses.clear()

        val redirectHtml = """<html>No redirect here</html>"""
        responses.add(redirectHtml to 200)

        val client = createMockClient()
        val server = Voe(
            url = "https://voe.com/initial",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testMissingVideoUrl() = runTest {
        requestCount = 0
        responses.clear()

        val redirectHtml = """window.location.href = 'https://voe.com/actual-video'"""
        val videoHtml = """<html>No video URL here</html>"""

        responses.add(redirectHtml to 200)
        responses.add(videoHtml to 200)

        val client = createMockClient()
        val server = Voe(
            url = "https://voe.com/initial",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testFirstRequestFails() = runTest {
        requestCount = 0
        responses.clear()

        responses.add("" to 404)

        val client = createMockClient()
        val server = Voe(
            url = "https://voe.com/initial",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
