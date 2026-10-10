package com.ead.lib.moongetter.voe

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.voe.factory.VoeFactory
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoeLiveTest {
    // Opt-in: signed media URLs expire, so normal regression tests stay offline.
    @Test
    fun extractsAndValidatesLivePlaylist() = runBlocking {
        val target = System.getenv("MOONGETTER_VOE_TEST_URL") ?: return@runBlocking
        assertTrue(Regex(VoeFactory.pattern).matches(target))
        val client = object : MoonClient {
            override fun initConfigurationData(configData: Configuration.Data) = Unit
            override suspend fun <T> request(request: Request<T>): Response {
                val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
                try {
                    val status = connection.responseCode
                    val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()?.use { it.readText() }.orEmpty()
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
        val server = VoeFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), Configuration.Data(), client)
        assertFalse(server.isDeprecated)
        val video = server.onExtract().single()
        val source = video.request.url
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
