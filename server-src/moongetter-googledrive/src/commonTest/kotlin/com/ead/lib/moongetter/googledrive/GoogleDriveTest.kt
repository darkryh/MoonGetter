package com.ead.lib.moongetter.googledrive

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GoogleDriveTest {

    private fun createMockClient(responseBody: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient(Configuration.Data()) {
            override suspend fun GET() = createMockResponse(responseBody, statusCode)
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
    fun testDirectDownload206() = runTest {
        val client = createMockClient("", 206)
        val server = GoogleDrive(
            url = "https://drive.google.com/file/d/ABC123/view",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("ABC123"))
    }

    @Test
    fun testConfirmationFlow200() = runTest {
        val html = """
            <input name="id" value="file_id_123">
            <input name="export" value="download">
            <input name="confirm" value="confirm_token">
            <input name="uuid" value="uuid_value">
        """.trimIndent()

        val client = createMockClient(html, 200)
        val server = GoogleDrive(
            url = "https://drive.google.com/file/d/XYZ789/view",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("file_id_123"))
        assertTrue(videos[0].url.contains("confirm=confirm_token"))
        assertTrue(videos[0].url.contains("uuid=uuid_value"))
    }

    @Test
    fun testMissingConfirmationParameters() = runTest {
        val html = """
            <input name="id" value="file_id_123">
            <input name="export" value="download">
        """.trimIndent()

        val client = createMockClient(html, 200)
        val server = GoogleDrive(
            url = "https://drive.google.com/file/d/XYZ789/view",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testInvalidFileIdInUrl() = runTest {
        val client = createMockClient("", 200)

        assertFailsWith<InvalidServerException> {
            GoogleDrive(
                url = "https://drive.google.com/invalid",
                client = client,
                headers = hashMapOf(),
                configData = Configuration.Data()
            )
        }
    }

    @Test
    fun testUnsuccessfulResponse() = runTest {
        val client = createMockClient("", 404)
        val server = GoogleDrive(
            url = "https://drive.google.com/file/d/ABC123/view",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
