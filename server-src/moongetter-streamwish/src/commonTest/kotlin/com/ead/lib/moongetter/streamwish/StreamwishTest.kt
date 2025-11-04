package com.ead.lib.moongetter.streamwish

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
import kotlin.test.assertFalse

/**
 * Test suite for Streamwish server implementation.
 * Tests M3U8 extraction with unpacking.
 */
class StreamwishTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://streamwish.to/e/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Streamwish(
            url = "https://streamwish.to/e/test",
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
        val server = Streamwish(
            url = "https://streamwish.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noPackedJS() = runTest {
        val html = """<html><body><div>No packed JS</div></body></html>"""

        val client = createMockClient(html)
        val server = Streamwish(
            url = "https://streamwish.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testHeaders_userAgentRemoved() {
        val headers = hashMapOf("User-Agent" to "Test", "Referer" to "https://example.com")
        val url = "https://streamwish.to/e/test"
        val server = Streamwish(
            url = url,
            client = createMockClient(""),
            headers = headers,
            configData = Configuration.Data()
        )

        assertFalse(server.headers.containsKey("User-Agent"))
        assertEquals(url, server.headers["Origin"])
        assertEquals(url, server.headers["Referer"])
    }
}
