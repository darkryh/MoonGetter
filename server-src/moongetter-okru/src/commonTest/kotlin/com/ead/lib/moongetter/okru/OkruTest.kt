package com.ead.lib.moongetter.okru

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OkruTest {

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
    fun testSuccessfulDataOptionsExtraction() = runTest {
        val html = """
            <div data-options="{&quot;flashvars&quot;:{&quot;metadata&quot;:&quot;{\&quot;videos\&quot;:[{\&quot;name\&quot;:\&quot;sd\&quot;,\&quot;url\&quot;:\&quot;https://example.com/video.mp4\&quot;}]}&quot;}}"></div>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Okru(
            url = "https://okru.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos.isNotEmpty())
    }

    @Test
    fun testMultipleQualitiesExtraction() = runTest {
        val html = """
            <div data-options="{&quot;flashvars&quot;:{&quot;metadata&quot;:&quot;{\&quot;videos\&quot;:[{\&quot;name\&quot;:\&quot;sd\&quot;,\&quot;url\&quot;:\&quot;https://example.com/480p.mp4\&quot;},{\&quot;name\&quot;:\&quot;hd\&quot;,\&quot;url\&quot;:\&quot;https://example.com/720p.mp4\&quot;}]}&quot;}}"></div>
        """.trimIndent()

        val client = createMockClient(html)
        val server = Okru(
            url = "https://okru.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos.size >= 2)
    }

    @Test
    fun testMissingDataOptions() = runTest {
        val html = "<html><body>No data-options here</body></html>"

        val client = createMockClient(html)
        val server = Okru(
            url = "https://okru.com/video",
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
        val server = Okru(
            url = "https://okru.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
