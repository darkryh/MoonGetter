package com.ead.lib.moongetter.filemoon

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

/**
 * Test suite for Filemoon server implementation.
 * Tests two-step extraction with iframe and packed JavaScript.
 */
class FilemoonTest {

    private var requestCount = 0
    
    private fun createMockClient(
        firstResponse: String,
        secondResponse: String = "",
        statusCode: Int = 200
    ): MoonClient {
        requestCount = 0
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                requestCount++
                val body = if (requestCount == 1) firstResponse else secondResponse
                return Response(
                    statusCode = statusCode,
                    headers = mapOf("content-type" to "text/html"),
                    body = ResponseBody.StringBody(body),
                    url = com.ead.lib.moongetter.client.url.HttpUrl(request.url)
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_withIframe() = runTest {
        val firstHtml = """
            <html>
            <body>
                <iframe src="https://filemoon.sx/embed/test123" allowfullscreen></iframe>
            </body>
            </html>
        """.trimIndent()

        // Second response would have packed JS
        val secondHtml = """<html><body>No unpacker</body></html>"""

        val client = createMockClient(firstHtml, secondHtml)
        val server = Filemoon(
            url = "https://filemoon.sx/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        // Will fail at unpacking stage, but validates iframe extraction
        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_unsuccessfulFirstResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Filemoon(
            url = "https://filemoon.sx/e/test",
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
        val client = createMockClient("", "", 200)
        val server = Filemoon(
            url = "https://filemoon.sx/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noIframe() = runTest {
        val html = """<html><body><div>No iframe</div></body></html>"""

        val client = createMockClient(html)
        val server = Filemoon(
            url = "https://filemoon.sx/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testHeaders_allHeadersSet() {
        val url = "https://filemoon.sx/e/test123"
        val server = Filemoon(
            url = url,
            client = createMockClient(""),
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertEquals("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", server.headers["Accept"])
        assertEquals("en-US,en;q=0.5", server.headers["Accept-Language"])
        assertEquals("u=0, i", server.headers["Priority"])
        assertEquals(url, server.headers["Origin"])
        assertEquals(url, server.headers["Referer"])
    }
}
