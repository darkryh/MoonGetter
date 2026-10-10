package com.ead.lib.moongetter.onecloudfile

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class OneCloudFileTest {
    private val source = "https://fs21.1cloudfile.com/Qhji/test.mp4?download_token=signed"
    private fun page(value: String = source) = "l(\"${value.map { (it.code xor 0x7a).toString(16).padStart(2, '0') }.joinToString("")}\",\"DOWNLOAD FILE (1 MB)\")"

    @Test fun decodesDownloadAndRejectsUnsupportedEncoding() {
        assertEquals(source, OneCloudPayload.source(page()))
        assertNull(OneCloudPayload.source("l('abc','DOWNLOAD FILE')"))
        assertNull(OneCloudPayload.source(page("https://evil.example/video.mp4")))
        assertNull(OneCloudPayload.source(page("https://fs21.1cloudfile.com.evil/video.mp4")))
        assertNull(OneCloudPayload.source(page("https://fs21.1cloudfile.com/poster.jpg")))
    }

    @Test fun factoryIsAnchored() {
        assertTrue(Regex(OneCloudPayload.PATTERN).matches("https://1cloudfile.com/Qhji"))
        assertFalse(Regex(OneCloudPayload.PATTERN).matches("https://1cloudfile.com.evil/Qhji"))
    }

    @Test fun extractsWithoutRobotAndPreservesTicketAndHeaders() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val original = hashMapOf("origin" to "old", "User-Agent" to "test")
        val result = OneCloudFile("https://1cloudfile.com/Qhji", fake(calls), original, Configuration.Data()).onExtract().single().request
        assertEquals(source, result.url)
        assertEquals(2, calls.size)
        assertEquals(HttpMethod.GET, calls.last().method)
        assertFalse(calls.last().isResponseBodyNeeded)
        assertEquals("https://1cloudfile.com/Qhji", result.headers?.get("Referer"))
        assertEquals("old", original["origin"])
    }

    @Test fun rejectsPlaceholderAndUnrelatedRedirect() = runBlocking {
        assertFailsWith<InvalidServerException> { OneCloudFile("https://1cloudfile.com/Qhji", fake(mutableListOf(), type = "text/html"), hashMapOf(), Configuration.Data()).onExtract() }
        assertFailsWith<InvalidServerException> { OneCloudFile("https://1cloudfile.com/Qhji", fake(mutableListOf(), canonical = "https://evil.example/Qhji"), hashMapOf(), Configuration.Data()).onExtract() }
        Unit
    }

    private fun fake(calls: MutableList<Request<*>>, type: String = "video/mp4", canonical: String = "https://1cloudfile.com/Qhji") = object : MoonClient {
        override fun initConfigurationData(configData: Configuration.Data) {}
        override suspend fun <T> request(request: Request<T>): Response {
            calls += request
            return object : Response {
                override val statusCode = 200
                override val headers = mapOf("content-type" to type)
                override val url = object : Url { override val host = "1cloudfile.com"; override val toString = if (calls.size == 1) canonical else source }
                override val body = object : ResponseBody { override fun asString() = page() }
            }
        }
    }
}
