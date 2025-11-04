package com.ead.lib.moongetter.mediafire

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.ResponseBody
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test suite for Mediafire server implementation.
 * Tests Base64 decoding of scrambled URLs from data attributes.
 */
@OptIn(ExperimentalEncodingApi::class)
class MediafireTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://www.mediafire.com/file/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_scrambledUrl() = runTest {
        val videoUrl = "https://download123.mediafire.com/video.mp4"
        val encodedUrl = Base64.encode(videoUrl.encodeToByteArray())
        
        val html = """
            <html>
            <body>
                <a href="#" aria-label="Download file" data-scrambled-url="$encodedUrl">Download</a>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals(videoUrl, videos[0].url)
    }

    @Test
    fun testExtract_multilinePattern() = runTest {
        val videoUrl = "https://cdn.mediafire.com/downloads/video123.mp4"
        val encodedUrl = Base64.encode(videoUrl.encodeToByteArray())
        
        val html = """
            <html>
            <body>
                <div class="download">
                    <a href="#" 
                       aria-label="Download file" 
                       class="btn primary"
                       data-scrambled-url="$encodedUrl">
                        Download Now
                    </a>
                </div>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals(videoUrl, videos[0].url)
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
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
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noDownloadLink() = runTest {
        val html = """
            <html>
            <body>
                <div>File not found</div>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
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
        val videoUrl = "https://download789.mediafire.com/files/abc123/video_file.mp4"
        val encodedUrl = Base64.encode(videoUrl.encodeToByteArray())
        
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>MediaFire</title></head>
            <body>
                <div id="content">
                    <h1>Download Video</h1>
                    <div class="buttons">
                        <a href="#" class="preview">Preview</a>
                        <a href="#" 
                           aria-label="Download file" 
                           class="download-button"
                           data-scrambled-url="$encodedUrl">
                            Download
                        </a>
                    </div>
                </div>
                <script src="analytics.js"></script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals(videoUrl, videos[0].url)
    }

    @Test
    fun testExtract_urlWithSpecialCharacters() = runTest {
        val videoUrl = "https://download.mediafire.com/files/test_video-2024.mp4?auth=token"
        val encodedUrl = Base64.encode(videoUrl.encodeToByteArray())
        
        val html = """
            <html>
            <a href="#" aria-label="Download file" data-scrambled-url="$encodedUrl">Download</a>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("test_video-2024.mp4"))
        assertTrue(videos[0].url.contains("auth=token"))
    }

    @Test
    fun testHeaders_userAgentRemoved() {
        val headers = hashMapOf("User-Agent" to "Test", "Referer" to "https://example.com")
        val server = Mediafire(
            url = "https://www.mediafire.com/file/test",
            client = createMockClient(""),
            headers = headers,
            configData = Configuration.Data()
        )

        // Verify User-Agent was removed
        assertTrue(!server.headers.containsKey("User-Agent"))
        assertTrue(server.headers.containsKey("Referer"))
    }
}
