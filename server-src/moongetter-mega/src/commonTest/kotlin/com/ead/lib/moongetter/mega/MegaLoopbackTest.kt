package com.ead.lib.moongetter.mega
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.test.*

class MegaLoopbackTest {
    @Test fun realLocalServerSupportsHeadRangesAndSessionIsolation()=runBlocking {
        val http=loopbackHttpClient()
        fun upstream()=HttpClient(MockEngine { request ->
            if(request.url.host=="g.api.mega.co.nz") respond("""[{"s":256,"at":"wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs","g":"https://test.mega.co.nz/ticket"}]""")
            else respond(MegaLink.decode("nYPaa5FULSQ03KC5ix8zrzumUGMk-qhNjWDTTrK3bZrjsU4Vs-A88ZB3yCgzDd4yAvOxrLqDWs6O8iexXgTNguN0S2L0IXO_Rm6022CiZmLqVxjcVMm3DJYWFlkhXEOD3nmk7-U5RGU_k_XjiWXnWnRNTyuzqb9edYMIyJNbHW4_mrfRL_frHnas-HO2Xj3Fsv0aDE0Lg0UuIU3WaksMlNzSjSe96jtsmYjmM9uBAtgrJ-jCI3Qe2iTCm_wFjcyUGc6iYpJJFUdlEZ4pMEe3E68u1lJZUoUZCOynbyYijF1Sot7ON7iuMnIdAP31OeyJpWzQESpiWfTQc9Atg7KmjQ"))
        })
        try {
            val source=MegaLink.parse("https://mega.nz/file/ABCDEFGH#AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8")
            val playback=MegaProxy.register(MegaSession.createWithClient(source,upstream()))
            val head=http.head(playback.url)
            assertEquals(HttpStatusCode.OK,head.status)
            assertEquals("256",head.headers[HttpHeaders.ContentLength])
            assertEquals("video/mp4",head.headers[HttpHeaders.ContentType])
            val range=http.get(playback.url) { header(HttpHeaders.Range,"bytes=113-255") }
            assertEquals(HttpStatusCode.PartialContent,range.status)
            assertEquals("bytes 113-255/256",range.headers[HttpHeaders.ContentRange])
            assertContentEquals(ByteArray(143) { (it+113).toByte() },range.bounded(143))
            assertEquals(HttpStatusCode.RequestedRangeNotSatisfiable,http.get(playback.url) { header(HttpHeaders.Range,"bytes=0-1,5-7") }.status)
            assertEquals(HttpStatusCode.NotFound,http.get(playback.url.replace("/video.mp4","/unknown")).status)
            val secondSource=MegaLink.parse("https://mega.nz/file/ABCDEFGH#AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8")
            val second=MegaProxy.register(MegaSession.createWithClient(secondSource,upstream()))
            assertNotEquals(playback.url,second.url)
            MegaProxy.release(playback.url)
            assertEquals(HttpStatusCode.NotFound,http.get(playback.url).status)
            assertTrue(source.secret.all { it==0.toByte() })
            assertEquals(HttpStatusCode.OK,http.head(second.url).status)
            MegaProxy.closeAll()
            assertTrue(secondSource.secret.all { it==0.toByte() })
            assertFails { http.head(second.url) };Unit
        } finally { http.close();MegaProxy.closeAll() }
    }
}
