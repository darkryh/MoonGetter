package com.ead.lib.moongetter.abyss

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.sync.*
import kotlin.time.TimeSource

internal class AbyssUpstreamException(val localStatus: Int): Exception()
internal class AbyssSession(private val source: AbyssSource, referer: String, headers: Map<String,String>, timeout: Long, private val http: HttpClient = abyssHttpClient(timeout)) {
    companion object { const val BLOCK = 262144L }
    val size = source.size
    val filename = "Abyss-${source.label}.mp4"
    val contentType = ContentType.Video.MP4
    private val requestHeaders = headers.filterKeys { !it.equals("Cookie",true) } + mapOf("Referer" to referer, "Origin" to "https://${Url(referer).host}")
    private val gate = Mutex()
    private val activity = Mutex()
    private var lastAccess = TimeSource.Monotonic.markNow()
    private var closed = false
    private var prefix: ByteArray? = null
    suspend fun touch() = activity.withLock { lastAccess = TimeSource.Monotonic.markNow() }
    suspend fun isIdle(duration: Long) = activity.withLock { lastAccess.elapsedNow().inWholeMilliseconds > duration }
    suspend fun close() = gate.withLock { closed = true; http.close(); prefix?.fill(0); prefix = null }
    suspend fun read(start: Long, end: Long): ByteArray = gate.withLock {
        check(!closed) { "Abyss playback has been released" }
        require(start >= 0 && end in start until size && end-start < BLOCK)
        val result = ByteArray((end-start+1).toInt())
        var position = start
        var dest = 0
        if (position < 65536) {
            val first = prefix ?: run {
                val raw = fetch(0, minOf(65535,size-1))
                val decrypted = abyssDecrypt(source.url.substringAfterLast('/'),raw)
                check(decrypted.size >= 12 && decrypted.copyOfRange(4,8).decodeToString() == "ftyp") { "Abyss prefix is not an MP4" }
                decrypted.also { prefix = it }
            }
            val count = (minOf(end,first.size-1L)-position+1).toInt()
            first.copyInto(result,dest,position.toInt(),position.toInt()+count)
            position += count; dest += count
        }
        while (position <= end) {
            val last = minOf(end, ((position/source.partSize)+1)*source.partSize-1)
            val bytes = fetch(position,last)
            bytes.copyInto(result,dest);dest+=bytes.size;position=last+1
        }
        result
    }
    private suspend fun fetch(start: Long, end: Long): ByteArray {
        val part = start/source.partSize
        val offset = part*source.partSize
        val address = source.url + if(part == 0L) "" else part.toString()
        return http.prepareGet(address) {
            requestHeaders.forEach { (key,value) -> header(key,value) }
            header(HttpHeaders.Range,"bytes=${start-offset}-${end-offset}")
        }.execute { response ->
            if(response.status != HttpStatusCode.PartialContent) throw AbyssUpstreamException(if(response.status.value == 403) 403 else 502)
            val expected = "bytes ${start-offset}-${end-offset}/${minOf(source.partSize,size-offset)}"
            check(response.headers[HttpHeaders.ContentRange] == expected) { "Invalid Abyss upstream range" }
            response.bounded((end-start+1).toInt())
        }
    }
}
internal data class AbyssRange(val start:Long,val end:Long) {
    val length get() = end-start+1
    companion object {
        fun parse(header:String?,size:Long): AbyssRange? {
            if(header == null) return AbyssRange(0,size-1)
            val m=Regex("bytes=(\\d*)-(\\d*)").matchEntire(header.trim()) ?: return null
            val a=m.groupValues[1];val b=m.groupValues[2]
            if(a.isEmpty()) { val n=b.toLongOrNull()?.takeIf { it>0 } ?: return null;return AbyssRange((size-n).coerceAtLeast(0),size-1) }
            val start=a.toLongOrNull()?.takeIf { it in 0 until size } ?: return null
            val end=if(b.isEmpty()) size-1 else b.toLongOrNull()?.takeIf { it>=start }?.coerceAtMost(size-1) ?: return null
            return AbyssRange(start,end)
        }
    }
}
internal suspend fun HttpResponse.bounded(limit:Int):ByteArray {
    val channel=bodyAsChannel()
    val output=ByteArray(limit)
    var offset=0
    while(offset<limit) { val read=channel.readAvailable(output,offset,limit-offset);if(read<0)break;offset+=read }
    check(offset==limit) { "Incomplete Abyss upstream body" }
    return output
}
