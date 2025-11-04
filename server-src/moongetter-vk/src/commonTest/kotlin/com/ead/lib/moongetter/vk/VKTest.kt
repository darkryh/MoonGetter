package com.ead.lib.moongetter.vk

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VKTest {

    private fun createMockClient(responseBody: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient(Configuration.Data()) {
            override suspend fun GET(overrideHeaders: Map<String, String>?) = createMockResponse(responseBody, statusCode)
        }
    }

    private fun createMockResponse(body: String, statusCode: Int = 200) = object {
        val isSuccess = statusCode in 200..299
        val statusCode = statusCode
        val body = object {
            fun asString() = body
        }
    }

    @Test
    fun testSuccessfulVideoExtraction() = runTest {
        val html = """
            {"url720":"https://example.com/video720.mp4"}
        """.trimIndent()

        val client = createMockClient(html)
        val server = VK(
            url = "https://vk.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertEquals("720", videos[0].quality)
    }

    @Test
    fun testMultipleQualitiesExtraction() = runTest {
        val html = """
            {"url480":"https://example.com/video480.mp4","url720":"https://example.com/video720.mp4","url1080":"https://example.com/video1080.mp4"}
        """.trimIndent()

        val client = createMockClient(html)
        val server = VK(
            url = "https://vk.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos.size >= 3)
    }

    @Test
    fun testNoVideoFound() = runTest {
        val html = """{"someother":"data"}"""

        val client = createMockClient(html)
        val server = VK(
            url = "https://vk.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testUnsuccessfulResponse() = runTest {
        val client = createMockClient("", 404)
        val server = VK(
            url = "https://vk.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testEmptyResponse() = runTest {
        val client = createMockClient("")
        val server = VK(
            url = "https://vk.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
