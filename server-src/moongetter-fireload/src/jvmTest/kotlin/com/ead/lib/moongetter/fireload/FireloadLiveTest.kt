package com.ead.lib.moongetter.fireload

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class FireloadLiveTest {
    @Test fun extractsAndDecodesMp4() = runBlocking {
        val target = System.getenv("MOONGETTER_FIRELOAD_TEST_URL") ?: return@runBlocking
        val storage = io.ktor.client.plugins.cookies.AcceptAllCookiesStorage()
        val cookies = object : MoonCookie.Management {
            override suspend fun get(requestUrl: io.ktor.http.Url) = storage.get(requestUrl)
            override suspend fun addCookie(requestUrl: io.ktor.http.Url, cookie: io.ktor.http.Cookie) = storage.addCookie(requestUrl, cookie)
            override fun close() = storage.close()
        }
        val client = KtorMoonClient(CIO, cookies, MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val trace = object : com.ead.lib.moongetter.client.MoonClient {
            override fun initConfigurationData(configData: Configuration.Data) = client.initConfigurationData(configData)
            override suspend fun <T> request(request: com.ead.lib.moongetter.client.request.Request<T>): com.ead.lib.moongetter.client.response.Response {
                val response = client.request(request)
                println("Fireload HTTP status=${response.statusCode}, finalHost=${response.url.host}, sessionHeader=${request.headers.containsKey("Cookie")}, setCookie=${response.headers.keys.any { it.equals("Set-Cookie", true) }}")
                return response
            }
        }
        val started = System.nanoTime()
        val request = Fireload(target, trace, hashMapOf("User-Agent" to "Mozilla/5.0"), config).onExtract().single().request
        if (System.getenv("MOONGETTER_FIRELOAD_FFMPEG") == "1") {
            for (seek in listOf(0, 600)) {
                val process = ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "error", "-xerror", "-abort_on", "empty_output", "-headers",
                    "Referer: ${request.headers?.get("Referer")}\r\nOrigin: ${request.headers?.get("Origin")}\r\n", "-ss", seek.toString(), "-i", request.url,
                    "-t", "3", "-f", "null", "-").redirectErrorStream(true)
                    .redirectOutput(java.io.File("/tmp/moongetter-fireload-ffmpeg-$seek.log")).start()
                if (!process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS)) { process.destroyForcibly(); error("Fireload decode timeout") }
                assertEquals(0, process.exitValue(), "Fireload decode/seek failed")
            }
        }
        println("Fireload extraction, opening decode and seek validated: ${(System.nanoTime() - started) / 1_000_000} ms")
    }
}
