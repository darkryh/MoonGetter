package com.ead.lib.moongetter.goodstream

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GoodStreamTest {

    private fun createMockClient(responseBody: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient(Configuration.Data()) {
            override suspend fun GET() = createMockResponse(responseBody, statusCode)
        }
    }

    private fun createMockResponse(body: String, statusCode: Int = 200) = object {
        val isSuccess = statusCode in 200..299
        val statusCode = statusCode
        val body = object {
            fun asString() = body
        }
    }

    @Test
    fun testSuccessfulVideoExtraction() = runTest {
        val html = """
            <html>
            <script>
                file: "https://example.com/video.mp4"
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = GoodStream(
            url = "https://goodstream.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertEquals("https://example.com/video.mp4", videos[0].request.url)
    }

    @Test
    fun testWithQueryParameters() = runTest {
        val html = """file: "https://example.com/video.mp4?token=abc123&expires=456""""

        val client = createMockClient(html)
        val server = GoodStream(
            url = "https://goodstream.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertTrue(videos[0].request.url.contains("token=abc123"))
    }

    @Test
    fun testUnsuccessfulResponse() = runTest {
        val client = createMockClient("", 404)
        val server = GoodStream(
            url = "https://goodstream.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testEmptyResponse() = runTest {
        val client = createMockClient("")
        val server = GoodStream(
            url = "https://goodstream.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testNoVideoFound() = runTest {
        val html = "<html><body>No video here</body></html>"

        val client = createMockClient(html)
        val server = GoodStream(
            url = "https://goodstream.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
