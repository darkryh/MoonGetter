package com.ead.lib.moongetter.vimeos

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.vimeos.factory.VimeosFactory
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VimeosLiveTest {
    // Opt-in: signed media URLs expire, so normal regression tests stay offline.
    @Test
    fun extractsAndValidatesLivePlaylist() = runBlocking {
        val target = System.getenv("MOONGETTER_VIMEOS_TEST_URL") ?: return@runBlocking
        assertTrue(Regex(VimeosFactory.pattern).matches(target))
        var pageMillis = 0L
        val client = object : MoonClient {
            override fun initConfigurationData(configData: Configuration.Data) = Unit
            override suspend fun <T> request(request: Request<T>): Response {
                val pageStarted = System.nanoTime()
                val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                try {
                    val status = connection.responseCode
                    val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
                    pageMillis = (System.nanoTime() - pageStarted) / 1_000_000
                    val finalUrl = connection.url.toString()
                    return object : Response {
                        override val statusCode = status
                        override val headers = emptyMap<String, String>()
                        override val body = object : ResponseBody { override fun asString() = text }
                        override val url = object : Url {
                            override val toString = finalUrl
                            override val host = URI(finalUrl).host
                        }
                    }
                } finally { connection.disconnect() }
            }
        }
        val server = VimeosFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), Configuration.Data(), client)
        assertFalse(server.isDeprecated)
        val extractionStarted = System.nanoTime()
        val video = server.onExtract().single()
        val totalMillis = (System.nanoTime() - extractionStarted) / 1_000_000
        println("Vimeos timing: page=${pageMillis}ms, unpack/overhead=${totalMillis - pageMillis}ms, total=${totalMillis}ms")
        val source = video.request.url
        System.getenv("MOONGETTER_VIMEOS_SOURCE_FILE")?.let { java.io.File(it).writeText(source) }
        if (System.getenv("MOONGETTER_VIMEOS_VALIDATE_MEDIA") != "1") return@runBlocking
        assertTrue(URI(source).path.endsWith(".m3u8"))
        assertFalse(source.contains("test-videos.co.uk"))
        val connection = URI(source).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        video.request.headers?.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        try {
            assertEquals(200, connection.responseCode)
            val playlist = connection.inputStream.bufferedReader().use { it.readText() }
            assertTrue(playlist.startsWith("#EXTM3U"))
            assertTrue(playlist.contains("#EXT-X-STREAM-INF"))
        } finally { connection.disconnect() }
    }
}
