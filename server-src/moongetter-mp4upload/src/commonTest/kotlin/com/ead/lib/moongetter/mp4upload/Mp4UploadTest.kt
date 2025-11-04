package com.ead.lib.moongetter.mp4upload

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
 * Test suite for Mp4Upload server implementation.
 * Tests extraction patterns and validation logic.
 */
class Mp4UploadTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://www.mp4upload.com/embed-test.html")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_srcPattern() = runTest {
        val html = """
            <html>
            <script>
            var player = jwplayer('vplayer').setup({
                src: "https://cdn.mp4upload.com/videos/video123.mp4",
                type: "video/mp4"
            });
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://cdn.mp4upload.com/videos/video123.mp4", videos[0].request?.url)
        assertEquals("GET", videos[0].request?.method)
    }

    @Test
    fun testExtract_filePattern() = runTest {
        val html = """
            <html>
            <script>
            var player = {
                file: "https://cdn.mp4upload.com/videos/video456.mp4"
            };
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].request?.url?.contains(".mp4") == true)
    }

    @Test
    fun testExtract_sourceTagFallback() = runTest {
        val html = """
            <html>
            <video>
                <source src="https://cdn.mp4upload.com/videos/video789.mp4" type="video/mp4">
            </video>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://cdn.mp4upload.com/videos/video789.mp4", videos[0].request?.url)
    }

    @Test
    fun testExtract_withQueryParams() = runTest {
        val html = """
            <html>
            <script>
            var player = jwplayer('vplayer').setup({
                src: "https://cdn.mp4upload.com/videos/video.mp4?token=abc123&quality=720p",
                type: "video/mp4"
            });
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        val videoUrl = videos[0].request?.url ?: ""
        assertTrue(videoUrl.contains("token=abc123"))
        assertTrue(videoUrl.contains("quality=720p"))
    }

    @Test
    fun testExtract_validationRejectsNonHttp() = runTest {
        val html = """
            <html>
            <script>
            var player = jwplayer('vplayer').setup({
                src: "video.mp4",
                type: "video/mp4"
            });
            </script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 500)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
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
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
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
                <div>No video content</div>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
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
            <head><title>Mp4Upload</title></head>
            <body>
                <div id="player"></div>
                <script src="jquery.js"></script>
                <script>
                var config = {
                    autoplay: false,
                    controls: true
                };
                var player = jwplayer('vplayer').setup({
                    src: "https://s1.mp4upload.com/d/xyz123/video_hd.mp4",
                    type: "video/mp4",
                    image: "poster.jpg"
                });
                </script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mp4Upload(
            url = "https://www.mp4upload.com/embed-test.html",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://s1.mp4upload.com/d/xyz123/video_hd.mp4", videos[0].request?.url)
        assertEquals("Default", videos[0].quality)
    }

    @Test
    fun testHeaders_setCorrectly() = runTest {
        val html = """
            <script>
            var player = jwplayer('vplayer').setup({
                src: "https://cdn.mp4upload.com/test.mp4"
            });
            </script>
        """.trimIndent()

        val url = "https://www.mp4upload.com/embed-test.html"
        val client = createMockClient(html)
        val server = Mp4Upload(
            url = url,
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        // Check that headers are set correctly
        assertEquals(url, server.headers["Referer"])
        assertEquals(url, server.headers["Origin"])
    }
}
