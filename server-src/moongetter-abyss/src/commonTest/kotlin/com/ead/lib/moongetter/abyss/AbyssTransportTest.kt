package com.ead.lib.moongetter.abyss

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class AbyssTransportTest {
    @Test fun decryptsPrefixAndReconstructsRangesAcrossParts(): Unit = runBlocking {
        val path="a/b/c/01234567890123456789012345678.131072.3"
        val plain=ByteArray(131072) { (it%251).toByte() }
        "ftyp".encodeToByteArray().copyInto(plain,4)
        val encrypted=plain.copyOf()
        abyssDecrypt(path.substringAfterLast('/'),plain.copyOfRange(0,65536)).copyInto(encrypted)
        val http=HttpClient(MockEngine { request ->
            assertEquals("https://argtesa.top",request.headers["Origin"])
            assertNull(request.headers["Cookie"])
            val part=if(request.url.encodedPath.endsWith(".31")) 1 else 0
            val range=Regex("bytes=(\\d+)-(\\d+)").matchEntire(request.headers["Range"]!!)!!
            val start=range.groupValues[1].toInt();val end=range.groupValues[2].toInt()
            respond(encrypted.copyOfRange(part*65536+start,part*65536+end+1),HttpStatusCode.PartialContent,
                headersOf(HttpHeaders.ContentRange,"bytes $start-$end/65536"))
        })
        val session=AbyssSession(AbyssSource("https://media.sssrr.org/$path",131072,65536,"480p"),"https://argtesa.top/?v=video123",mapOf("Cookie" to "private"),60000,http)
        try {
            assertContentEquals(plain.copyOfRange(0,32),session.read(0,31))
            assertContentEquals(plain.copyOfRange(65520,65552),session.read(65520,65551))
            assertContentEquals(plain.copyOfRange(131040,131072),session.read(131040,131071))
        } finally { session.close() }
        assertFailsWith<IllegalStateException> { session.read(0,31) }
    }
    @Test fun rejectsIncorrectUpstreamRange(): Unit = runBlocking {
        val http=HttpClient(MockEngine { respond(ByteArray(32),HttpStatusCode.PartialContent,headersOf(HttpHeaders.ContentRange,"bytes 0-31/999")) })
        val session=AbyssSession(AbyssSource("https://media.sssrr.org/a/b/c/01234567890123456789012345678.131072.3",131072,65536,"480p"),"https://argtesa.top/?v=video123",emptyMap(),60000,http)
        try { assertFailsWith<IllegalStateException> { session.read(65536,65567) } } finally { session.close() }
    }
}
