package com.ead.lib.moongetter.vidguard

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
 * Test suite for Vidguard server implementation.
 * Tests script extraction and execution with obfuscated data.
 */
class VidguardTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://vidguard.to/e/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Vidguard(
            url = "https://vidguard.to/e/test",
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
        val server = Vidguard(
            url = "https://vidguard.to/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noScriptWithEval() = runTest {
        val html = """<html><body><div>No script tag</div></body></html>"""

        val client = createMockClient(html)
        val server = Vidguard(
            url = "https://vidguard.to/e/test",
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
        val url = "https://vidguard.to/e/test123"
        val server = Vidguard(
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
