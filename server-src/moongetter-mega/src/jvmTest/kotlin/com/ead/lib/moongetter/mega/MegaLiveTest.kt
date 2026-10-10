package com.ead.lib.moongetter.mega
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.test.*

class MegaLiveTest {
    @Test fun realProxyDecryptsRangesSeeksAndDecodesVideo()=runBlocking {
        val link=System.getenv("MOONGETTER_MEGA_TEST_URL") ?: return@runBlocking
        val started=System.nanoTime()
        val http=megaHttpClient(60_000)
        try {
            if(System.getenv("MOONGETTER_MEGA_EXPECT_AMBIGUOUS")=="1") {
                val bare=link.substringBefore("/file/",link)
                val failure=assertFailsWith<IllegalStateException> { MegaProxy.open(bare) }
                assertTrue(failure.message.orEmpty().contains("videos"))
                println("MEGA ambiguous folder rejected without opening a playback session")
            }
            val occupied=java.net.ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"))
            val session=try { MegaProxy.open(link) } finally { occupied.close() }
            assertNotEquals(occupied.localPort,Url(session.url).port)
            assertTrue(session.url.startsWith("http://127.0.0.1:"))
            suspend fun range(value:String):Pair<io.ktor.client.statement.HttpResponse,ByteArray> {
                val r=http.get(session.url) { header(HttpHeaders.Range,value) }
                return r to r.bounded(4096)
            }
            val (head,bytes)=range("bytes=0-4095")
            assertEquals(HttpStatusCode.PartialContent,head.status)
            assertEquals("ftyp",bytes.copyOfRange(4,8).decodeToString())
            val (seek,overlap)=range("bytes=113-255")
            assertEquals(HttpStatusCode.PartialContent,seek.status)
            assertContentEquals(bytes.copyOfRange(113,256),overlap)
            val (_,tail)=range("bytes=-1024");assertEquals(1024,tail.size)
            assertEquals(HttpStatusCode.RequestedRangeNotSatisfiable,http.get(session.url) { header(HttpHeaders.Range,"bytes=${session.size}-") }.status)
            assertEquals(HttpStatusCode.NotFound,http.get(session.url.replace("/video.mp4","/missing")).status)
            if(System.getenv("MOONGETTER_MEGA_FFMPEG")=="1") {
                suspend fun decode(vararg arguments:String)=withContext(Dispatchers.IO) {
                    val command=listOf("ffmpeg","-hide_banner","-loglevel","error","-xerror","-probesize","65536","-analyzeduration","1000000")+arguments+listOf("-f","null","-")
                    val process=ProcessBuilder(command).redirectErrorStream(true).redirectOutput(java.io.File("/tmp/moongetter-mega-ffmpeg.log")).start()
                    if(!process.waitFor(15,java.util.concurrent.TimeUnit.MINUTES)) { process.destroyForcibly();error("Decoder timeout") }
                    assertEquals(0,process.exitValue(),"Decoder failed; inspect /tmp/moongetter-mega-ffmpeg.log")
                }
                val decodeStart=System.nanoTime()
                decode("-i",session.url,"-t","3")
                println("MEGA initial 3-second decode: ${(System.nanoTime()-decodeStart)/1_000_000} ms")
                val seekStart=System.nanoTime()
                decode("-ss","600","-i",session.url,"-t","3")
                println("MEGA 10-minute seek/3-second decode: ${(System.nanoTime()-seekStart)/1_000_000} ms")
                if(System.getenv("MOONGETTER_MEGA_FULL_DECODE")=="1") {
                    val fullStart=System.nanoTime()
                    decode("-i",session.url)
                    println("MEGA full video/audio decode: ${(System.nanoTime()-fullStart)/1_000_000} ms")
                }
            }
            MegaProxy.release(session.url)
            assertEquals(HttpStatusCode.NotFound,http.get(session.url).status)
            println("MEGA live loopback/MP4/range/seek/release validated: ${(System.nanoTime()-started)/1_000_000} ms, ${session.size} bytes total file size. Device playback requires separate validation.")
        } finally { http.close();MegaProxy.closeAll() }
    }
}
