package com.ead.lib.moongetter.rpmstream

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import com.ead.lib.moongetter.rpmstream.factory.RpmstreamFactory
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class RpmstreamLiveTest {
    @Test fun extractsAndDecodesNativeHls() = runBlocking {
        val target = System.getenv("MOONGETTER_RPMSTREAM_TEST_URL") ?: return@runBlocking
        val client = KtorMoonClient(CIO, MoonCookie.Management.newEmptyFactory(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val started = System.nanoTime()
        val request = RpmstreamFactory.create(target, hashMapOf("User-Agent" to "Mozilla/5.0"), config, client)
            .onExtract().single().request
        suspend fun get(url: String) = client.request<String>(Request(method = HttpMethod.GET, url = url, headers = request.headers.orEmpty()))
        val master = get(request.url)
        assertTrue(master.isSuccess)
        val variant = master.body.asString().lineSequence().first { it.isNotBlank() && !it.startsWith('#') }
        val variantUrl = java.net.URI(request.url).resolve(variant).toString()
        val media = get(variantUrl)
        assertTrue(media.isSuccess)
        assertTrue(media.body.asString().startsWith("#EXTM3U"))
        if (System.getenv("MOONGETTER_RPMSTREAM_FFMPEG") == "1") {
            val process = ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-xerror",
                "-allowed_extensions", "ALL", "-extension_picky", "0",
                "-headers", "Referer: ${request.headers?.get("Referer")}\r\n", "-i", request.url,
                "-t", "3", "-f", "null", "-")
                .redirectErrorStream(true).redirectOutput(java.io.File("/tmp/moongetter-rpmstream-ffmpeg.log")).start()
            if (!process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); error("RPMStream decoder timeout") }
            assertEquals(0, process.exitValue(), "RPMStream media decode failed")
        }
        println("RPMStream native HLS and variant validated: ${(System.nanoTime() - started) / 1_000_000} ms")
    }
}
