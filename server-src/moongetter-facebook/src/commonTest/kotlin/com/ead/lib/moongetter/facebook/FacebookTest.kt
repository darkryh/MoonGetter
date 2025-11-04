package com.ead.lib.moongetter.facebook

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FacebookTest {

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
    fun testSuccessfulSDLinkExtraction() = runTest {
        val html = """
            <a id="sdlink" href="https://example.com/video_sd.mp4">Download SD</a>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Facebook(
            url = "https://facebook.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertEquals("https://example.com/video_sd.mp4", videos[0].url)
    }

    @Test
    fun testFallbackToHDLink() = runTest {
        val html = """
            <a id="hdlink" href="https://example.com/video_hd.mp4">Download HD</a>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Facebook(
            url = "https://facebook.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertEquals("https://example.com/video_hd.mp4", videos[0].url)
    }

    @Test
    fun testAmpersandReplacement() = runTest {
        val html = """
            <a id="sdlink" href="https://example.com/video.mp4?param1=value1&amp;param2=value2">Download</a>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Facebook(
            url = "https://facebook.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos[0].url.contains("&param2="))
        assertTrue(!videos[0].url.contains("&amp;"))
    }

    @Test
    fun testNoVideoFound() = runTest {
        val html = "<html><body>No video links here</body></html>"

        val client = createMockClient(html)
        val server = Facebook(
            url = "https://facebook.com/video",
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
        val server = Facebook(
            url = "https://facebook.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
