package com.ead.lib.moongetter.mailru

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import com.ead.lib.moongetter.mailru.factory.MailruFactory
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class MailruLiveTest {
    @Test fun extractsAndValidatesMedia() = runBlocking {
        val target = System.getenv("MOONGETTER_MAILRU_TEST_URL") ?: return@runBlocking
        val client = KtorMoonClient(CIO, MoonCookie.Management.newEmptyFactory(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val started = System.nanoTime()
        val videos = MailruFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), config, client).onExtract()
        assertTrue(videos.isNotEmpty())
        val request = videos.first().request
        val headers = request.headers.orEmpty()
        suspend fun fetch(url: String, range: Boolean = false) = client.request<String>(Request(
            method = HttpMethod.GET, url = url,
            headers = headers + if (range) mapOf("Range" to "bytes=0-1023") else emptyMap()
        ))
        val media = fetch(request.url, range = true)
        assertTrue(media.isSuccess)
        assertTrue(media.body.asString().isNotEmpty())
        println("Mail.ru source and initial media request validated: ${(System.nanoTime() - started) / 1_000_000} ms; ${videos.size} source(s); media HTTP ${media.statusCode}.")
    }
}
