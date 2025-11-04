package com.ead.lib.moongetter.yourupload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.ResponseBody
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test suite for YourUpload server implementation.
 * Tests extraction patterns and error handling with mocked HTTP responses.
 */
class YourUploadTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://test.yourupload.com/embed/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_singleQuotesPattern() = runTest {
        val html = """
            <html>
            <script>
            var player = {
                file: 'https://example.com/video.mp4'
            };
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://example.com/video.mp4", videos[0].url)
        assertEquals("https://www.yourupload.com/", videos[0].headers["Referer"])
    }

    @Test
    fun testExtract_doubleQuotesPattern() = runTest {
        val html = """
            <html>
            <script>
            var player = {
                file: "https://example.com/video.mp4"
            };
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://example.com/video.mp4", videos[0].url)
    }

    @Test
    fun testExtract_sourceTagFallback() = runTest {
        val html = """
            <html>
            <video>
                <source src="https://example.com/video.mp4" type="video/mp4">
            </video>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://example.com/video.mp4", videos[0].url)
    }

    @Test
    fun testExtract_withQueryParams() = runTest {
        val html = """
            <html>
            <script>
            var player = {
                file: 'https://example.com/video.mp4?token=abc123&quality=720p'
            };
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("token=abc123"))
        assertTrue(videos[0].url.contains("quality=720p"))
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_emptyResponse() = runTest {
        val client = createMockClient("", statusCode = 200)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noVideoFound() = runTest {
        val html = """
            <html>
            <body>
                <div>No video content here</div>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_complexHtml() = runTest {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Video Player</title>
            </head>
            <body>
                <div id="player"></div>
                <script>
                var config = {
                    autoplay: true,
                    file: 'https://cdn.example.com/videos/video123.mp4',
                    image: 'poster.jpg'
                };
                </script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = YourUpload(
            url = "https://test.yourupload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://cdn.example.com/videos/video123.mp4", videos[0].url)
    }
}
