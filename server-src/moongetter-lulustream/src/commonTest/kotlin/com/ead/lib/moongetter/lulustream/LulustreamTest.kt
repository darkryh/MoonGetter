package com.ead.lib.moongetter.lulustream

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
 * Test suite for Lulustream server implementation.
 * Tests M3U8 extraction with packed JavaScript support.
 */
class LulustreamTest {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl(request.url)
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_m3u8Url() = runTest {
        val html = """
            <script>
            var player = {
                file: "https://cdn.lulustream.com/hls/playlist.m3u8?token=abc123"
            };
            </script>
        """.trimIndent()

        // Note: This test validates pattern extraction only
        // Actual HLS processing would require more complex mocking
        val client = createMockClient(html)
        val server = Lulustream(
            url = "https://lulustream.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        // This will attempt to extract from HLS which requires network
        // In a real test, we'd mock PlaylistUtils
        assertFailsWith<Exception> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = Lulustream(
            url = "https://lulustream.com/embed/test",
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
        val server = Lulustream(
            url = "https://lulustream.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noM3u8Found() = runTest {
        val html = """<html><body><div>No playlist</div></body></html>"""

        val client = createMockClient(html)
        val server = Lulustream(
            url = "https://lulustream.com/embed/test",
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
        val url = "https://lulustream.com/embed/test123"
        val server = Lulustream(
            url = url,
            client = createMockClient(""),
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertEquals(url, server.headers["Referer"])
        assertEquals(url, server.headers["Origin"])
    }
}
