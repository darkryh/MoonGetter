package com.ead.project.moongetter

import com.ead.lib.moongetter.MoonFactory
import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.cookie.java.net.JavaNetCookieManagement
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.trust.manager.java.net.JavaMoonTrustManager
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.server.bundle.serverBundle
import com.ead.project.moongetter.app.network.TimedMoonClient
import com.ead.project.moongetter.app.lib.httpClientEngineFactory
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.TimeSource

class ColdWarmExtractionTest {
    /** Opt-in diagnostic: first and repeated extraction share the sample's client. */
    @Test
    fun compareFirstAndRepeatedExtraction(): Unit = runBlocking {
        val url = System.getenv("MOONGETTER_SAMPLE_TIMING_URL") ?: return@runBlocking
        val initialization = TimeSource.Monotonic.markNow()
        MoonGetter.start(MoonFactory.Builder()
            .setClient(TimedMoonClient(KtorMoonClient(httpClientEngineFactory, JavaNetCookieManagement(), JavaMoonTrustManager)))
            .setTimeout(60_000L)
            .setEngine(Engine.Builder().onCore(serverBundle).build()))
        println("Client configuration duration: ${initialization.elapsedNow()}")
        repeat(2) { attempt ->
            val opened = mutableListOf<String>()
            val started = TimeSource.Monotonic.markNow()
            try {
                val server = withTimeout(60_000L) { MoonGetter.get(url) }
                opened += server.videos.map { it.request.url }
                assertTrue(server.videos.isNotEmpty())
                println("Source resolved ${attempt + 1}: ${started.elapsedNow()}, videos=${server.videos.size}")
                if (System.getenv("MOONGETTER_SAMPLE_VALIDATE_MEDIA") == "1") {
                    val request = server.videos.first().request
                    val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 15_000
                    request.headers?.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                    connection.setRequestProperty("Range", "bytes=0-0")
                    val mediaStarted = TimeSource.Monotonic.markNow()
                    try {
                        val status = connection.responseCode
                        assertTrue(status == 200 || status == 206, "Media request HTTP $status")
                        val type = connection.contentType.orEmpty().substringBefore(';').lowercase()
                        assertTrue(type.startsWith("video/") || type in setOf("application/octet-stream", "application/vnd.apple.mpegurl", "application/x-mpegurl"), "Unexpected media type: $type")
                        connection.inputStream.use { assertTrue(it.read() >= 0, "Media returned no bytes") }
                        println("Media access ${attempt + 1}: ${mediaStarted.elapsedNow()}, status=$status, type=$type")
                    } finally { connection.disconnect() }
                }
                println("Extraction ${attempt + 1}: ${started.elapsedNow()}, videos=${server.videos.size}")
            } catch (failure: Exception) {
                println("Extraction ${attempt + 1}: ${started.elapsedNow()}, error=${failure::class.simpleName}")
                throw failure
            } finally {
                for (local in opened.filter { it.startsWith("http://127.0.0.1:") }) {
                    com.ead.lib.moongetter.abyss.AbyssProxy.release(local)
                    com.ead.lib.moongetter.mega.MegaProxy.release(local)
                }
                com.ead.lib.moongetter.abyss.AbyssProxy.closeAll()
                com.ead.lib.moongetter.mega.MegaProxy.closeAll()
            }
        }
    }
}
