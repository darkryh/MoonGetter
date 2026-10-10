package com.ead.lib.moongetter.onecloudfile

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class OneCloudFileLiveTest {
    @Test fun extractsAndDecodesMp4() = runBlocking {
        val target = System.getenv("MOONGETTER_ONECLOUD_TEST_URL") ?: return@runBlocking
        val client = KtorMoonClient(CIO, MoonCookie.Management.newEmptyFactory(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val started = System.nanoTime()
        val request = OneCloudFile(target, client, hashMapOf("User-Agent" to "Mozilla/5.0"), config).onExtract().single().request
        if (System.getenv("MOONGETTER_ONECLOUD_FFMPEG") == "1") {
            for (seek in listOf(0, 10)) {
                val process = ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-xerror", "-headers",
                    "Referer: ${request.headers?.get("Referer")}\r\nOrigin: ${request.headers?.get("Origin")}\r\n", "-ss", seek.toString(), "-i", request.url,
                    "-t", "3", "-f", "null", "-").redirectErrorStream(true)
                    .redirectOutput(java.io.File("/tmp/moongetter-onecloud-ffmpeg-$seek.log")).start()
                if (!process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); error("1CloudFile decode timeout") }
                assertEquals(0, process.exitValue(), "1CloudFile decode/seek failed")
            }
        }
        println("1CloudFile extraction, opening decode and seek validated: ${(System.nanoTime() - started) / 1_000_000} ms")
    }
}
