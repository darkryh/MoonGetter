package com.ead.lib.moongetter.voe

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import com.ead.lib.moongetter.voe.factory.VoeFactory
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class VoeColdStartTest {
    @Test fun comparesFirstAndRepeatedKtorExtraction() = runBlocking {
        val target = System.getenv("MOONGETTER_VOE_TEST_URL") ?: return@runBlocking
        val started = System.nanoTime()
        val client = KtorMoonClient(CIO, MoonCookie.Management.newEmptyFactory(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 12000, disableSSLValidation = false)
        client.initConfigurationData(config)
        println("Ktor construction/configuration: ${(System.nanoTime() - started) / 1_000_000} ms")
        repeat(3) { index ->
            val extractionStarted = System.nanoTime()
            val video = VoeFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), config, client)
                .onExtract().single()
            assertTrue(video.request.url.contains(".m3u8"))
            println("VOE Ktor extraction ${index + 1}: ${(System.nanoTime() - extractionStarted) / 1_000_000} ms")
        }
    }
}
