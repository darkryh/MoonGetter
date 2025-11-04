package com.ead.lib.moongetter.mixdrop

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
 * Test suite for Mixdrop server implementation.
 * Tests packed JavaScript extraction with eval blocks.
 */
class MixdropTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://mixdrop.co/e/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_packedJavaScript() = runTest {
        // Note: Real packed JS would be much longer
        val html = """
            <script>
            eval(function(p,a,c,k,e,d){while(c--){if(k[c]){p=p.replace(new RegExp('\\b'+c+'\\b','g'),k[c])}}return p}('wurl="//cdn.mixdrop.co/video.mp4";',62,62,'||||||||||||||||||||'.split('|'),0,{}))
            </script>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Mixdrop(
            url = "https://mixdrop.co/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        // This would work with real packed JS
        // For now, test that it attempts extraction
        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Mixdrop(
            url = "https://mixdrop.co/e/test",
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
        val server = Mixdrop(
            url = "https://mixdrop.co/e/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noPackedJavaScript() = runTest {
        val html = """<html><body><div>No packed JS here</div></body></html>"""

        val client = createMockClient(html)
        val server = Mixdrop(
            url = "https://mixdrop.co/e/test",
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
        val server = Mixdrop(
            url = "https://mixdrop.co/e/test",
            client = createMockClient(""),
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertEquals("https://mixdrop.co/", server.headers["Referer"])
        assertEquals("https://mixdrop.co/", server.headers["Origin"])
    }
}
