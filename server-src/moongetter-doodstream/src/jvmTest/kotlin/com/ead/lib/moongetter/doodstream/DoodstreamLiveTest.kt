package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import com.ead.lib.moongetter.doodstream.factory.DoodstreamFactory
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class DoodstreamLiveTest {
    @Test fun extractsAndRequestsMedia() = runBlocking {
        val target = System.getenv("MOONGETTER_DOODSTREAM_TEST_URL") ?: return@runBlocking
        val client = KtorMoonClient(CIO, MoonCookie.Management.newEmptyFactory(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val started = System.nanoTime()
        val videos = try {
            DoodstreamFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), config, client).onExtract()
        } catch (error: InvalidServerException) {
            println("Doodstream live extraction failed after ${(System.nanoTime() - started) / 1_000_000} ms: ${error.error}; ${error.message}")
            throw error
        }
        assertTrue(videos.isNotEmpty())
        val video = videos.first().request
        val media = client.request<String>(Request(method = HttpMethod.GET, url = video.url,
            headers = video.headers.orEmpty() + ("Range" to "bytes=0-1023")))
        assertTrue(media.isSuccess)
        assertTrue(media.body.asString().isNotEmpty())
        println("Doodstream extraction and initial media request succeeded in ${(System.nanoTime() - started) / 1_000_000} ms; HTTP ${media.statusCode}")
    }
}
