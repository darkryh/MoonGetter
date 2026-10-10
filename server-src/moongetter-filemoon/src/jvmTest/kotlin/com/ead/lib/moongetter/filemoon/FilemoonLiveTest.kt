package com.ead.lib.moongetter.filemoon

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import com.ead.lib.moongetter.filemoon.factory.FilemoonFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class FilemoonLiveTest {
    @Test fun extractsProtectedFrontendAndValidatesMedia() = runBlocking {
        val target = System.getenv("MOONGETTER_FILEMOON_PROTECTED_TEST_URL") ?: return@runBlocking
        val client = KtorMoonClient(CIO, MoonCookie.Management.newEmptyFactory(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val started = System.nanoTime()
        val videos = FilemoonFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), config, client).onExtract()
        assertTrue(videos.isNotEmpty())
        val request = videos.first().request
        val headers = request.headers.orEmpty()
        suspend fun fetch(url: String, range: Boolean = false) = client.request<String>(Request(
            method = HttpMethod.GET, url = url,
            headers = headers + if (range) mapOf("Range" to "bytes=0-1023") else emptyMap()
        ))
        fun resolve(base: String, path: String) = URLBuilder(base).takeFrom(path).buildString()
        val master = fetch(request.url)
        assertTrue(master.isSuccess)
        val masterText = master.body.asString()
        assertTrue(masterText.startsWith("#EXTM3U"))
        val firstUri = masterText.lineSequence().map(String::trim).first { it.isNotEmpty() && !it.startsWith("#") }
        val variantUrl = resolve(master.url.toString, firstUri)
        val variant = fetch(variantUrl)
        assertTrue(variant.isSuccess)
        val variantText = variant.body.asString()
        assertTrue(variantText.startsWith("#EXTM3U"))
        val segment = variantText.lineSequence().map(String::trim).first { it.isNotEmpty() && !it.startsWith("#") }
        val bytes = fetch(resolve(variant.url.toString, segment), range = true)
        assertTrue(bytes.isSuccess)
        assertTrue(bytes.body.asString().isNotEmpty())
        println("Filemoon/Byse HTTP extraction and master/variant/initial media request validated: ${(System.nanoTime() - started) / 1_000_000} ms; ${videos.size} source(s); media HTTP ${bytes.statusCode}. Android first frame requires device testing.")
    }
}
