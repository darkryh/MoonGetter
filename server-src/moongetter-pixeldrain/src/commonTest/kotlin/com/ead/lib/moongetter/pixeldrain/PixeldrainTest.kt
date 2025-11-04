package com.ead.lib.moongetter.pixeldrain

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
 * Test suite for Pixeldrain server implementation.
 * Tests ID extraction and direct download URL construction.
 */
class PixeldrainTest {

    private fun createMockClient(statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(""),
                    url = com.ead.lib.moongetter.client.url.HttpUrl(request.url)
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_standardUrl() = runTest {
        val client = createMockClient(statusCode = 200)
        val server = Pixeldrain(
            url = "https://pixeldrain.com/u/abc123xyz",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("https://pixeldrain.com/api/file/abc123xyz?download", videos[0].url)
    }

    @Test
    fun testExtract_alphanumericId() = runTest {
        val client = createMockClient(statusCode = 200)
        val server = Pixeldrain(
            url = "https://pixeldrain.com/u/aBc123XyZ789",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("aBc123XyZ789"))
        assertTrue(videos[0].url.contains("/api/file/"))
        assertTrue(videos[0].url.endsWith("?download"))
    }

    @Test
    fun testExtract_invalidUrl() = runTest {
        val client = createMockClient(statusCode = 200)
        val server = Pixeldrain(
            url = "https://pixeldrain.com/invalid",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient(statusCode = 404)
        val server = Pixeldrain(
            url = "https://pixeldrain.com/u/abc123",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_serverError() = runTest {
        val client = createMockClient(statusCode = 500)
        val server = Pixeldrain(
            url = "https://pixeldrain.com/u/test123",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_urlWithTrailingPath() = runTest {
        val client = createMockClient(statusCode = 200)
        val server = Pixeldrain(
            url = "https://pixeldrain.com/u/xyz789abc/preview",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("xyz789abc"))
    }

    @Test
    fun testExtract_constructsCorrectApiUrl() = runTest {
        val client = createMockClient(statusCode = 200)
        val testId = "TestId123"
        val server = Pixeldrain(
            url = "https://pixeldrain.com/u/$testId",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals("https://pixeldrain.com/api/file/$testId?download", videos[0].url)
    }
}
