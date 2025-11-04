package com.ead.lib.moongetter.lamovie

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LaMovieTest {

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
    fun testSuccessfulM3U8Extraction() = runTest {
        // Packed JavaScript with M3U8 URL
        val packedJs = """
            eval(function(p,a,c,k,e,d){while(c--)if(k[c])p=p.replace(new RegExp('\\b'+c+'\\b','g'),k[c]);return p}('const url="https://example.com/playlist.m3u8";',0,0,''.split('|'),0,{}))
        """.trimIndent()

        val html = """
            <html>
            <script>$packedJs</script>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = LaMovie(
            url = "https://lamovie.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos.isNotEmpty())
    }

    @Test
    fun testM3U8WithQueryParameters() = runTest {
        val packedJs = """
            eval(function(p,a,c,k,e,d){return 'https://example.com/playlist.m3u8?token=abc123'}('',0,0,''.split('|'),0,{}))
        """.trimIndent()

        val html = """<script>$packedJs</script>"""

        val client = createMockClient(html)
        val server = LaMovie(
            url = "https://lamovie.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()
        assertTrue(videos.isNotEmpty())
    }

    @Test
    fun testNonPackedContent() = runTest {
        val html = "<html><body>No packed JavaScript here</body></html>"

        val client = createMockClient(html)
        val server = LaMovie(
            url = "https://lamovie.com/video",
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
        val server = LaMovie(
            url = "https://lamovie.com/video",
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
        val server = LaMovie(
            url = "https://lamovie.com/video",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }
}
