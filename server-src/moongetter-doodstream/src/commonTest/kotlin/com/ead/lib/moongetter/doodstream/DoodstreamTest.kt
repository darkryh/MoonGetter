package com.ead.lib.moongetter.doodstream

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
 * Test suite for Doodstream server implementation.
 * Tests multi-step extraction with keys and tokens.
 */
class DoodstreamTest {

    private var requestCount = 0
    
    private fun createMockClient(
        firstResponse: String,
        secondResponse: String = "",
        thirdResponse: String = "",
        statusCode: Int = 200
    ): MoonClient {
        requestCount = 0
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                requestCount++
                val body = when (requestCount) {
                    1 -> firstResponse
                    2 -> secondResponse
                    3 -> thirdResponse
                    else -> ""
                }
                return Response(
                    statusCode = statusCode,
                    headers = mapOf("content-type" to "text/html"),
                    body = ResponseBody.StringBody(body),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://dood.to/e/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_successfulExtraction() = runTest {
        val firstHtml = """
            <script>
            dsplayer.hotkeys = '/pass_md5/abc123/function';
            function makePlay() {
                return '/token/xyz789"
            }
            </script>
        """.trimIndent()

        val secondHtml = "https://cdn.dood.to/video_part"
        val thirdHtml = ""

        val client = createMockClient(firstHtml, secondHtml, thirdHtml)
        val server = Doodstream(
            url = "https://dood.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("https://cdn.dood.to/video_part"))
        assertTrue(videos[0].url.contains("/token/xyz789"))
    }

    @Test
    fun testExtract_unsuccessfulFirstRequest() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Doodstream(
            url = "https://dood.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_emptyFirstResponse() = runTest {
        val client = createMockClient("", "", "", 200)
        val server = Doodstream(
            url = "https://dood.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_missingKeysCode() = runTest {
        val html = """
            <script>
            function makePlay() {
                return '/token/xyz789"
            }
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Doodstream(
            url = "https://dood.to/e/test",
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
            <script>
            dsplayer.hotkeys = '/pass_md5/abc123/function';
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Doodstream(
            url = "https://dood.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testHeaders_originAndRefererSet() {
        val url = "https://dood.to/e/test123"
        val server = Doodstream(
            url = url,
            client = createMockClient(""),
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertEquals(url, server.headers["Origin"])
        assertEquals("/$url", server.headers["Referer"])
    }
}
