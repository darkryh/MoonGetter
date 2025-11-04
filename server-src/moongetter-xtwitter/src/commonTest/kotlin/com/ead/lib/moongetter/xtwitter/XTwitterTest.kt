package com.ead.lib.moongetter.xtwitter

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class XTwitterTest {

    private fun createMockClient(responseBody: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient(Configuration.Data()) {
            override suspend fun POST(
                requestUrl: String,
                body: Any?,
                asFormUrlEncoded: Boolean
            ) = createMockResponse(responseBody, statusCode)
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
            <table>
                <tr><td><a href="https://example.com/1280x720/video.mp4">Download</a></td></tr>
            </table>
        """.trimIndent()

        val client = createMockClient(html)
        val server = XTwitter(
            url = "https://twitter.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("1280x720"))
    }

    @Test
    fun testMultipleQualitiesExtraction() = runTest {
        val html = """
            <table>
                <tr><td><a href="https://example.com/640x360/video.mp4">Download</a></td></tr>
                <tr><td><a href="https://example.com/1280x720/video.mp4">Download</a></td></tr>
                <tr><td><a href="https://example.com/1920x1080/video.mp4">Download</a></td></tr>
            </table>
        """.trimIndent()

        val client = createMockClient(html)
        val server = XTwitter(
            url = "https://twitter.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos.size >= 3)
    }

    @Test
    fun testMP3AudioExtraction() = runTest {
        val html = """
            <table>
                <tr><td><a href="https://example.com/mp3.php?file=audio.mp3">Download</a></td></tr>
            </table>
        """.trimIndent()

        val client = createMockClient(html)
        val server = XTwitter(
            url = "https://twitter.com/audio",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertEquals("mp3", videos[0].quality)
    }

    @Test
    fun testNoVideoFound() = runTest {
        val html = "<html><body>No videos here</body></html>"

        val client = createMockClient(html)
        val server = XTwitter(
            url = "https://twitter.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testUnsuccessfulResponse() = runTest {
        val client = createMockClient("", 404)
        val server = XTwitter(
            url = "https://twitter.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
