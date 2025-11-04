package com.ead.lib.moongetter.hexload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.ResponseBody
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Test suite for Hexload server implementation.
 * Tests two-step extraction with data object and POST request.
 */
class HexloadTest {

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
    fun testExtract_unsuccessfulFirstResponse() = runTest {
        Values.targetUrl2 = "https://hexload.com/api"
        val client = createMockClient("", statusCode = 404)
        val server = Hexload(
            url = "https://hexload.com/e/test",
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
        Values.targetUrl2 = "https://hexload.com/api"
        val client = createMockClient("", "", 200)
        val server = Hexload(
            url = "https://hexload.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noDataObject() = runTest {
        Values.targetUrl2 = "https://hexload.com/api"
        val html = """<html><body><div>No data object</div></body></html>"""

        val client = createMockClient(html)
        val server = Hexload(
            url = "https://hexload.com/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
