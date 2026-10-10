package com.ead.lib.moongetter.mega
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.test.*

class MegaTransportTest {
    private val link get()=MegaLink.parse("https://mega.nz/file/ABCDEFGH#AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8")
    private val metadata="""[{"s":256,"at":"wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs","g":"https://test.mega.co.nz/ticket"}]"""
    private val encrypted get()=MegaLink.decode("nYPaa_MlUlM03KC5ix8zrzumUGMk-qhNjWDTTrK3bZrjsU4Vs-A88ZB3yCgzDd4yAvOxrLqDWs6O8iexXgTNguN0S2L0IXO_Rm6022CiZmLqVxjcVMm3DJYWFlkhXEOD3nmk7-U5RGU_k_XjiWXnWnRNTyuzqb9edYMIyJNbHW4_mrfRL_frHnas-HO2Xj3Fsv0aDE0Lg0UuIU3WaksMlNzSjSe96jtsmYjmM9uBAtgrJ-jCI3Qe2iTCm_wFjcyUGc6iYpJJFUdlEZ4pMEe3E68u1lJZUoUZCOynbyYijF1Sot7ON7iuMnIdAP31OeyJpWzQESpiWfTQc9Atg7KmjQ")
    @Test fun refreshesExpiredTicketOnceAndCachesBoundedBlock()=runBlocking {
        var tickets=0;var fetches=0
        val http=HttpClient(MockEngine { request ->
            if(request.url.host=="g.api.mega.co.nz") { tickets++;respond(metadata) }
            else { fetches++;if(fetches==1) respond("",HttpStatusCode.Forbidden) else respond(encrypted) }
        })
        val session=MegaSession.createWithClient(link,http)
        try {
            assertContentEquals(ByteArray(143) { (it+113).toByte() },session.read(113,255))
            assertContentEquals(ByteArray(32) { it.toByte() },session.read(0,31))
            assertEquals(2,tickets);assertEquals(2,fetches)
        } finally { session.close() }
    }
    @Test fun cacheEvictsAfterTwoBlocks()=runBlocking {
        val source=link
        val crypto=MegaCipher(source)
        val bigMetadata=metadata.replace("256,", "${MegaSession.BLOCK*3},")
        var reads=0
        val http=HttpClient(MockEngine { request ->
            if(request.url.host=="g.api.mega.co.nz") respond(bigMetadata)
            else {
                reads++
                val start=request.url.encodedPath.substringAfterLast('/').substringBefore('-').toLong()
                val bytes=ByteArray(MegaSession.BLOCK.toInt()) { it.toByte() }
                respond(crypto.decrypt(start,bytes))
            }
        })
        val session=MegaSession.createWithClient(source,http)
        try {
            for(offset in listOf(0L,MegaSession.BLOCK,2*MegaSession.BLOCK,0L)) assertContentEquals(byteArrayOf(0),session.read(offset,offset))
            assertEquals(4,reads)
            session.read(0,0);assertEquals(4,reads)
        } finally { session.close() }
    }
    @Test fun quotaDoesNotTriggerTicketRetry()=runBlocking {
        var calls=0
        val http=HttpClient(MockEngine { request -> calls++;if(request.url.host=="g.api.mega.co.nz") respond(metadata) else respond("",HttpStatusCode(509,"Quota")) })
        val session=MegaSession.createWithClient(link,http)
        try { assertEquals(429,assertFailsWith<MegaUpstreamException> { session.read(0,31) }.localStatus);assertEquals(2,calls) }
        finally {session.close()}
    }
    @Test fun truncatedCiphertextIsRejected()=runBlocking {
        val http=HttpClient(MockEngine { request -> if(request.url.host=="g.api.mega.co.nz") respond(metadata) else respond(encrypted.copyOfRange(0,32)) })
        val session=MegaSession.createWithClient(link,http)
        try { assertFailsWith<IllegalStateException> { session.read(0,31) };Unit } finally { session.close() }
    }
    @Test fun closeCancelsUpstreamAndClearsKey()=runBlocking {
        val entered=CompletableDeferred<Unit>()
        val source=link
        val http=HttpClient(MockEngine { request -> if(request.url.host=="g.api.mega.co.nz") respond(metadata) else { entered.complete(Unit);awaitCancellation() } })
        val session=MegaSession.createWithClient(source,http)
        val task=async { session.read(0,31) }
        entered.await();withTimeout(3000) { session.close() }
        assertFailsWith<CancellationException> { task.await() }
        assertTrue(source.secret.all { it==0.toByte() })
    }
}
