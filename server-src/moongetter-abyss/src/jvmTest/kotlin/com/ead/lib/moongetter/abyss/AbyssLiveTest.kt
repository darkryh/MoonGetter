package com.ead.lib.moongetter.abyss

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URI
import kotlin.test.*

class AbyssLiveTest {
    @Test fun extractsAndValidatesLocalRanges(): Unit = runBlocking {
        val target = System.getenv("MOONGETTER_ABYSS_TEST_URL") ?: return@runBlocking
        val storage = AcceptAllCookiesStorage()
        val cookies = object : MoonCookie.Management {
            override suspend fun get(requestUrl: Url) = storage.get(requestUrl)
            override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = storage.addCookie(requestUrl,cookie)
            override fun close() = storage.close()
        }
        val config=Configuration.Data(timeout=60_000)
        val client=KtorMoonClient(OkHttp,cookies,MoonTrust.Manager.newEmptyFactory())
        client.initConfigurationData(config)
        val started=System.nanoTime()
        try {
            val videos=Abyss(target,client,hashMapOf("User-Agent" to "Mozilla/5.0"),config).onExtract()
            assertTrue(videos.isNotEmpty())
            println("Abyss extraction: ${(System.nanoTime()-started)/1_000_000} ms, qualities=${videos.map { it.quality }}")
            val local=videos.first().request.url
            for(range in listOf("0-31","65520-65551","536870896-536870927")) {
                val c=URI(local).toURL().openConnection() as HttpURLConnection
                c.connectTimeout=15_000;c.readTimeout=15_000;c.setRequestProperty("Range","bytes=$range")
                try {
                    assertEquals(206,c.responseCode)
                    val bytes=c.inputStream.use { it.readBytes() }
                    assertEquals(32,bytes.size)
                    if(range=="0-31") assertEquals("ftyp",bytes.copyOfRange(4,8).decodeToString())
                } finally { c.disconnect() }
            }
            if(System.getenv("MOONGETTER_ABYSS_FFMPEG")=="1") {
                for(seek in listOf(0,600)) {
                    val p=ProcessBuilder("ffmpeg","-hide_banner","-loglevel","error","-xerror","-ss",seek.toString(),"-i",local,"-t","3","-f","null","-")
                        .redirectErrorStream(true).redirectOutput(java.io.File("/tmp/moongetter-abyss-ffmpeg-$seek.log")).start()
                    if(!p.waitFor(90,java.util.concurrent.TimeUnit.SECONDS)) {p.destroyForcibly();error("Abyss decode timed out")}
                    assertEquals(0,p.exitValue(),"Decode/seek failed")
                }
            }
        } finally { AbyssProxy.closeAll() }
    }
}
