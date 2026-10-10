package com.ead.lib.moongetter.abyss

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class AbyssLoopbackTest {
    @Test fun nativeClientAcceptsHeadAndRangeResponses(): Unit = runBlocking {
        val path = "a/b/c/01234567890123456789012345678.131072.3"
        val plain = ByteArray(131072) { (it % 251).toByte() }
        "ftyp".encodeToByteArray().copyInto(plain, 4)
        val encrypted = plain.copyOf()
        abyssDecrypt(path.substringAfterLast('/'), plain.copyOfRange(0, 65536)).copyInto(encrypted)
        val upstream = HttpClient(MockEngine { request ->
            val match = Regex("bytes=(\\d+)-(\\d+)").matchEntire(request.headers[HttpHeaders.Range]!!)!!
            val start = match.groupValues[1].toInt()
            val end = match.groupValues[2].toInt()
            respond(encrypted.copyOfRange(start, end + 1), HttpStatusCode.PartialContent,
                headersOf(HttpHeaders.ContentRange, "bytes $start-$end/131072"))
        })
        val client = abyssHttpClient(15000)
        try {
            val playback = AbyssProxy.register(AbyssSession(
                AbyssSource("https://media.sssrr.org/$path", 131072, 131072, "480p"),
                "https://argtesa.top/?v=video123", emptyMap(), 15000, upstream))
            val head = client.head(playback.url)
            assertEquals(HttpStatusCode.OK, head.status)
            assertEquals(listOf("131072"), head.headers.getAll(HttpHeaders.ContentLength))
            assertEquals(listOf("video/mp4"), head.headers.getAll(HttpHeaders.ContentType))
            val partialHead = client.head(playback.url) { header(HttpHeaders.Range, "bytes=0-31") }
            assertEquals(HttpStatusCode.PartialContent, partialHead.status)
            assertEquals(listOf("32"), partialHead.headers.getAll(HttpHeaders.ContentLength))
            val range = client.get(playback.url) { header(HttpHeaders.Range, "bytes=0-31") }
            assertEquals(HttpStatusCode.PartialContent, range.status)
            assertEquals(listOf("32"), range.headers.getAll(HttpHeaders.ContentLength))
            assertContentEquals(plain.copyOfRange(0, 32), range.bodyAsBytes())
        } finally {
            client.close()
            AbyssProxy.closeAll()
        }
    }
}
