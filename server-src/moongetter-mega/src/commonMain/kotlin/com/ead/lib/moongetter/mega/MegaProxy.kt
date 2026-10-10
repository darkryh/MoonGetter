package com.ead.lib.moongetter.mega

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import kotlinx.serialization.json.*
import kotlin.random.Random
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.time.TimeSource

/** App-owned loopback runtime. Release individual URLs after playback, and closeAll on app shutdown. */
object MegaProxy {
    private val lock=Mutex()
    private val sessions=mutableMapOf<String,MegaSession>()
    private var server:EmbeddedServer<CIOApplicationEngine,CIOApplicationEngine.Configuration>?=null
    private var port=0
    private var reaper:Job?=null
    private val permits=Semaphore(4)
    private const val IDLE_MILLIS=10*60*1000L

    class Playback internal constructor(val url:String,val filename:String,val size:Long)

    suspend fun open(link:String,timeout:Long=60_000):Playback {
        return register(MegaSession.create(link,timeout))
    }

    internal suspend fun register(session:MegaSession):Playback {
        try {
            return lock.withLock {
                check(sessions.size<4) { "Close an existing MEGA playback session before opening another" }
                if(server==null) {
                    val created=embeddedServer(CIO,host="127.0.0.1",port=0) {
                        routing {
                            head("/{session}/video.mp4") { serve(call,true) }
                            get("/{session}/video.mp4") { serve(call,false) }
                        }
                    }
                    try { created.start(wait=false);port=created.engine.resolvedConnectors().single().port;server=created }
                    catch(error:Throwable) { created.stop(0,1000);throw error }
                    reaper=CoroutineScope(SupervisorJob()+Dispatchers.Default).launch {
                        while(isActive) { delay(60_000);expireIdle() }
                    }
                }
                val id=CryptographyRandom.nextBytes(24).joinToString("") { (it.toInt() and 255).toString(16).padStart(2,'0') }
                sessions[id]=session
                sessionLock.withLock { visible[id]=session }
                val local="http://127.0.0.1:$port/$id/video.mp4"
                // Verify the actual listener and its exact session route before returning a playable URL.
                try {
                    val localClient=loopbackHttpClient()
                    try {
                    val probe=localClient.head(local)
                    check(probe.status==HttpStatusCode.OK && probe.headers[HttpHeaders.ContentLength]==session.size.toString()) { "Local MEGA listener validation failed" }
                    val media=localClient.get(local) { header(HttpHeaders.Range,"bytes=0-31") }
                    check(media.status==HttpStatusCode.PartialContent) { "Local MEGA range validation failed" }
                    val bytes=media.bounded(32)
                    check(bytes.size==32 && (bytes.copyOfRange(4,8).decodeToString()=="ftyp" || bytes.take(4)==listOf(0x1a.toByte(),0x45.toByte(),0xdf.toByte(),0xa3.toByte()))) { "MEGA decrypted file is not a supported MP4/WebM/MKV video" }
                } finally { localClient.close() }
                } catch(error:Throwable) { sessions.remove(id);sessionLock.withLock { visible.remove(id) };throw error }
                Playback(local,session.filename,session.size)
            }
        } catch(error:Throwable) { session.close();throw error }
    }

    fun releaseAsync(url:String) { CoroutineScope(Dispatchers.Default).launch { release(url) } }
    suspend fun release(url:String) {
        val parsed=runCatching { Url(url) }.getOrNull() ?: return
        if(parsed.host!="127.0.0.1" || parsed.protocol.name!="http" || parsed.segments.size!=2 || parsed.segments.last()!="video.mp4") return
        val id=parsed.segments.first()
        val old=lock.withLock { sessionLock.withLock { visible.remove(id) };sessions.remove(id) }
        old?.close()
    }
    suspend fun closeAll() {
        val old=lock.withLock {
            reaper?.cancel();reaper=null
            val all=sessions.values.toList();sessions.clear();sessionLock.withLock { visible.clear() }
            server?.stop(0,1000);server=null;port=0
            all
        }
        old.forEach { it.close() }
    }
    private suspend fun expireIdle() {
        val old=lock.withLock {
            val expired=sessions.filterValues { it.isIdle(IDLE_MILLIS) }.keys
            expired.mapNotNull { sessionLock.withLock { visible.remove(it) };sessions.remove(it) }
        }
        old.forEach { it.close() }
        withContext(NonCancellable) {
            lock.withLock {
                if(sessions.isEmpty()) { server?.stop(0,1000);server=null;port=0;reaper?.cancel();reaper=null }
            }
        }
    }
    private suspend fun serve(call:ApplicationCall,head:Boolean) {
        // Avoid taking the runtime mutex here: open() probes this route while owning it.
        val id=call.parameters["session"] ?: return call.respond(HttpStatusCode.NotFound)
        val session=lookup(id) ?: return call.respond(HttpStatusCode.NotFound)
        session.touch()
        val range=MegaRange.parse(call.request.headers[HttpHeaders.Range],session.size)
        call.response.header(HttpHeaders.AcceptRanges,"bytes")
        call.response.header(HttpHeaders.CacheControl,"no-store")
        if(range==null) { call.response.header(HttpHeaders.ContentRange,"bytes */${session.size}");return call.respond(HttpStatusCode.RequestedRangeNotSatisfiable) }
        val partial=call.request.headers[HttpHeaders.Range]!=null
        if(partial) call.response.header(HttpHeaders.ContentRange,"bytes ${range.start}-${range.end}/${session.size}")
        val status=if(partial) HttpStatusCode.PartialContent else HttpStatusCode.OK
        if(head) {
            // Let the engine write entity headers once. respond(status) also generates a body
            // and its own Content-Length, which Darwin rejects when combined with manual headers.
            return call.respond(object : OutgoingContent.NoContent() {
                override val status = status
                override val contentType = session.contentType
                override val contentLength = range.length
            })
        }
        // Fetch first block before committing HTTP headers so upstream/quota failures are reportable.
        val firstEnd=minOf(range.end,range.start+MegaSession.BLOCK-1)
        val first=try { session.read(range.start,firstEnd) } catch(cancelled:kotlinx.coroutines.CancellationException) { throw cancelled }
            catch(error:MegaUpstreamException) { return call.respond(HttpStatusCode.fromValue(error.localStatus)) }
            catch(_:Exception) { return call.respond(HttpStatusCode.BadGateway) }
        call.respondBytesWriter(contentType=session.contentType,status=status,contentLength=range.length) {
            permits.withPermit {
                writeFully(first)
                var position=firstEnd+1
                while(position<=range.end) {
                    currentCoroutineContext().ensureActive()
                    val end=minOf(range.end,position+MegaSession.BLOCK-1)
                    writeFully(session.read(position,end));position=end+1
                }
            }
        }
    }
    // The session map is replaced/read under a separate short mutex to support the startup probe.
    private suspend fun lookup(id:String):MegaSession?=sessionLock.withLock { visible[id] }
    private val sessionLock=Mutex()
    private val visible=mutableMapOf<String,MegaSession>()
}

internal class MegaUpstreamException(val localStatus:Int,message:String):Exception(message)

internal class MegaSession private constructor(private val link:MegaLink,val http:HttpClient) {
    private val cipher=MegaCipher(link)
    private val gate=Mutex()
    private var endpoint=""
    private val cache=linkedMapOf<Long,ByteArray>()
    var size=0L;private set
    var filename="";private set
    val contentType:ContentType get()=when(filename.substringAfterLast('.').lowercase()) {
        "webm" -> ContentType("video","webm")
        "mkv" -> ContentType("video","x-matroska")
        else -> ContentType.Video.MP4
    }
    private var lastAccess=TimeSource.Monotonic.markNow()
    private var closed=false
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
    private val activity=Mutex()
    suspend fun touch()=activity.withLock { lastAccess=TimeSource.Monotonic.markNow() }
    suspend fun isIdle(duration:Long)=activity.withLock { lastAccess.elapsedNow().inWholeMilliseconds>duration }
    companion object {
        const val BLOCK=1048576L
        suspend fun create(url:String,timeout:Long):MegaSession {
            val http=megaHttpClient(timeout.coerceIn(1000,60_000))
            val link=try { MegaFolder.resolve(url,http) } catch(error:Throwable) { http.close();throw error }
            return createWithClient(link,http)
        }
        internal suspend fun createWithClient(link:MegaLink,http:HttpClient):MegaSession {
            val session=MegaSession(link,http)
            try { session.ticket();return session } catch(error:Throwable) { session.close();throw error }
        }
    }
    private suspend fun ticket() {
        val response=http.post("https://g.api.mega.co.nz/cs") {
            parameter("id",Random.nextInt(1,Int.MAX_VALUE));contentType(ContentType.Application.Json)
            link.folderHandle?.let { parameter("n",it) }
            val handleField=if(link.folderHandle==null) "p" else "n"
            setBody("""[{"a":"g","g":1,"$handleField":"${link.handle}","ssl":1}]""")
        }
        val data=response.bounded(65536).decodeToString(throwOnInvalidSequence=true)
        val element=Json.parseToJsonElement(data).jsonArray.single()
        if(element is JsonPrimitive) throw MegaUpstreamException(502,"MEGA API error ${element.content}")
        val obj=element.jsonObject
        val newSize=obj["s"]?.jsonPrimitive?.longOrNull?.takeIf { it>0 } ?: error("MEGA file size missing")
        val attrs=obj["at"]?.jsonPrimitive?.content ?: error("MEGA encrypted metadata missing")
        val name=cipher.attributes(attrs)
        check(link.expectedName==null || (name==link.expectedName && newSize==link.expectedSize)) { "MEGA selected folder file changed" }
        check(size==0L || (size==newSize && filename==name)) { "MEGA file changed during streaming" }
        val raw=(obj["g"] as? JsonPrimitive)?.content ?: error("MEGA returned an unsupported striped download ticket")
        val candidate=if(raw.startsWith("http://")) "https://"+raw.removePrefix("http://") else raw
        val parsed=Url(candidate)
        require(parsed.protocol.name=="https" && parsed.host.endsWith(".mega.co.nz")) { "Untrusted MEGA download host" }
        endpoint=candidate.trimEnd('/');size=newSize;filename=name
    }
    suspend fun read(start:Long,end:Long):ByteArray=coroutineScope {
        val task=scope.async { gate.withLock {
        check(!closed) { "MEGA playback session closed" }
        require(start>=0 && end>=start && end<size && end-start+1<=BLOCK)
        currentCoroutineContext().ensureActive();touch()
        val result=ByteArray((end-start+1).toInt())
        var position=start
        while(position<=end) {
            val base=position-position%BLOCK
            val blockEnd=minOf(size-1,base+BLOCK-1)
            val plain=cache[base] ?: run {
                suspend fun fetch()=http.post("$endpoint/$base-$blockEnd")
                var response=fetch()
                if(response.status.value in listOf(403,404,410)) { ticket();response=fetch() }
                if(response.status.value==509 || response.status.value==429) throw MegaUpstreamException(429,"MEGA transfer quota or rate limit reached")
                if(response.status.value !in listOf(200,206)) throw MegaUpstreamException(502,"MEGA media HTTP ${response.status.value}")
                val encrypted=response.bounded((blockEnd-base+1).toInt())
                check(encrypted.size.toLong()==blockEnd-base+1) { "MEGA truncated range" }
                val decoded=cipher.decrypt(base,encrypted)
                if(cache.size==2) cache.remove(cache.keys.first())?.fill(0)
                cache[base]=decoded
                decoded
            }
            val count=(minOf(end,blockEnd)-position+1).toInt()
            plain.copyInto(result,(position-start).toInt(),(position-base).toInt(),(position-base).toInt()+count)
            position+=count
        }
        result
        } }
        try { task.await() } finally { task.cancel() }
    }
    suspend fun close()=withContext(NonCancellable) { scope.cancel();gate.withLock { if(!closed) { closed=true;http.close();link.clear();endpoint="";cache.values.forEach { it.fill(0) };cache.clear() } } }
}

internal suspend fun HttpResponse.bounded(limit:Int):ByteArray {
    val channel=bodyAsChannel()
    val bytes=ByteArray(limit+1)
    var offset=0
    try {
        while(offset<bytes.size) {
            val count=channel.readAvailable(bytes,offset,bytes.size-offset)
            if(count==-1) break
            if(count==0) { yield();continue }
            offset+=count
        }
        check(offset<=limit) { "Upstream response exceeds bounded buffer" }
        return bytes.copyOf(offset)
    } finally { channel.cancel() }
}

internal fun loopbackHttpClient()=HttpClient(ClientCIO) { install(HttpTimeout) { requestTimeoutMillis=60_000;connectTimeoutMillis=5000;socketTimeoutMillis=60_000 };followRedirects=false }
