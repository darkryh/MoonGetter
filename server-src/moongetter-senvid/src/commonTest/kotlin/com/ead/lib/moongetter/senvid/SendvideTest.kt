package com.ead.lib.moongetter.senvid

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
 * Test suite for Senvid server implementation.
 * Tests extraction patterns with source tag variations.
 */
class SendvideTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://senvid.net/embed/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_sourceTagDoubleQuotes() = runTest {
        val html = """
            <html>
            <video>
                <source src="https://cdn.senvid.net/video123.mp4" type="video/mp4">
            </video>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://cdn.senvid.net/video123.mp4", videos[0].url)
    }

    @Test
    fun testExtract_sourceTagSingleQuotes() = runTest {
        val html = """
            <html>
            <video>
                <source src='https://cdn.senvid.net/video456.mp4' type='video/mp4'>
            </video>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://cdn.senvid.net/video456.mp4", videos[0].url)
    }

    @Test
    fun testExtract_videoTag() = runTest {
        val html = """
            <html>
            <video src="https://cdn.senvid.net/video789.mp4" controls></video>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains(".mp4"))
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 403)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
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
        val server = Senvid(
            url = "https://senvid.net/embed/test",
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
                <div>No video here</div>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
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
            <head><title>Senvid Player</title></head>
            <body>
                <div id="player">
                    <video controls>
                        <source src="https://s1.senvid.net/videos/high_quality.mp4" type="video/mp4">
                        <p>Your browser doesn't support HTML5 video.</p>
                    </video>
                </div>
                <script src="player.js"></script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://s1.senvid.net/videos/high_quality.mp4", videos[0].url)
    }

    @Test
    fun testExtract_withQueryParams() = runTest {
        val html = """
            <html>
            <video>
                <source src="https://cdn.senvid.net/video.mp4?token=xyz&quality=720p" type="video/mp4">
            </video>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Senvid(
            url = "https://senvid.net/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("token=xyz"))
        assertTrue(videos[0].url.contains("quality=720p"))
    }
}
