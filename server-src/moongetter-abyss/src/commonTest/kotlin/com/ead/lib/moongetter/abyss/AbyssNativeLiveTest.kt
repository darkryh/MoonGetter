package com.ead.lib.moongetter.abyss

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

// Opt in by changing the target temporarily when diagnosing a provider; normal tests stay offline.
class AbyssNativeLiveTest {
    @Test fun validatesRemoteSourceThroughPlatformClient(): Unit = runBlocking {
        val target = liveTarget() ?: return@runBlocking
        val client = abyssHttpClient(60000)
        try {
            val response = client.get(target) { header(HttpHeaders.UserAgent, "Mozilla/5.0") }
            assertEquals(HttpStatusCode.OK, response.status)
            val data = assertNotNull(AbyssPayload.data(response.bodyAsText(), assertNotNull(AbyssPayload.slug(target))))
            val sources = AbyssPayload.sources(AbyssPayload.media(data))
            assertTrue(sources.isNotEmpty())
            val playback = AbyssProxy.register(AbyssSession(sources.first(), target,
                mapOf(HttpHeaders.UserAgent to "Mozilla/5.0"), 60000))
            for (range in listOf("0-31", "65520-65551", "536870896-536870927")) {
                val media = client.get(playback.url) { header(HttpHeaders.Range, "bytes=$range") }
                assertEquals(HttpStatusCode.PartialContent, media.status)
                assertEquals(32, media.bodyAsBytes().size)
            }
        } finally { client.close(); AbyssProxy.closeAll() }
    }
}
internal expect fun liveTarget(): String?
