package com.ead.lib.moongetter.uqload

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
 * Test suite for Uqload server implementation.
 * Tests sources array extraction and validation.
 */
class UqloadTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://uqload.com/embed/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_sourcesArrayDoubleQuotes() = runTest {
        val html = """
            <script>
            var player = jwplayer('player').setup({
                sources: ["https://cdn.uqload.com/video123.mp4"]
            });
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Uqload(
            url = "https://uqload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://cdn.uqload.com/video123.mp4", videos[0].request?.url)
        assertEquals("GET", videos[0].request?.method)
    }

    @Test
    fun testExtract_sourcesArraySingleQuotes() = runTest {
        val html = """
            <script>
            var player = jwplayer('player').setup({
                sources: ['https://cdn.uqload.com/video456.mp4']
            });
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Uqload(
            url = "https://uqload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].request?.url?.contains(".mp4") == true)
    }

    @Test
    fun testExtract_filePattern() = runTest {
        val html = """
            <script>
            var config = {
                file: "https://s1.uqload.com/video789.mp4"
            };
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Uqload(
            url = "https://uqload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].request?.url?.startsWith("https://") == true)
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 403)
        val server = Uqload(
            url = "https://uqload.com/embed/test",
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
        val server = Uqload(
            url = "https://uqload.com/embed/test",
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
        val html = """<html><body><div>No video</div></body></html>"""

        val client = createMockClient(html)
        val server = Uqload(
            url = "https://uqload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_validationRejectsNonHttp() = runTest {
        val html = """
            <script>
            var player = jwplayer('player').setup({
                sources: ["video.mp4"]
            });
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Uqload(
            url = "https://uqload.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testHeaders_refererAndOriginSet() {
        val url = "https://uqload.com/embed/test123"
        val server = Uqload(
            url = url,
            client = createMockClient(""),
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertEquals(url, server.headers["Referer"])
        assertEquals(url, server.headers["Origin"])
    }
}
