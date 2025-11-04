package com.ead.lib.moongetter.streamtape

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
 * Test suite for Streamtape server implementation.
 * Tests extraction patterns for Streamtape's unique URL construction.
 */
class StreamtapeTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://streamtape.com/e/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_standardPattern() = runTest {
        val html = """
            <html>
            <body>
                <div id="robotlink" style="display:none;">//domain.com/get_video?token=</div>
                <script>
                document.getElementById('robotlink').innerHTML = document.getElementById('robotlink').innerHTML + 'abc123xyz';
                </script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.startsWith("https:"))
        assertTrue(videos[0].url.contains("token="))
        assertTrue(videos[0].url.contains("abc123xyz"))
    }

    @Test
    fun testExtract_withComplexToken() = runTest {
        val html = """
            <html>
            <body>
                <div id="robotlink" style="display:none;">//streamtape.com/get_video?id=xyz&token=</div>
                <script>
                document.getElementById('robotlink').innerHTML = document.getElementById('robotlink').innerHTML + 'AbCdEfGh123456';
                </script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("AbCdEfGh123456"))
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 403)
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
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
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_missingRobotlink() = runTest {
        val html = """
            <html>
            <body>
                <div>No robotlink here</div>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_missingToken() = runTest {
        val html = """
            <html>
            <body>
                <div id="robotlink" style="display:none;">//domain.com/get_video?token=</div>
                <script>
                // No token assignment here
                </script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_complexHtmlWithOtherScripts() = runTest {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>Streamtape</title></head>
            <body>
                <div id="videoplayer">
                    <div id="robotlink" style="display:none;">//tape5.com/get_video?stream=xyz&token=</div>
                </div>
                <script src="player.js"></script>
                <script>
                var config = {autoplay: true};
                document.getElementById('robotlink').innerHTML = document.getElementById('robotlink').innerHTML + 'token123';
                </script>
                <script src="analytics.js"></script>
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Streamtape(
            url = "https://streamtape.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("token123"))
        assertTrue(videos[0].url.contains("stream=xyz"))
    }
}
