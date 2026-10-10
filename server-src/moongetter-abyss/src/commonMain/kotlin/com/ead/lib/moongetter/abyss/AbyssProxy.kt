package com.ead.lib.moongetter.abyss

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
object AbyssProxy {
    private val lock=Mutex()
    private val sessions=mutableMapOf<String,AbyssSession>()
    private var server:EmbeddedServer<CIOApplicationEngine,CIOApplicationEngine.Configuration>?=null
    private var port=0
    private var reaper:Job?=null
    private val permits=Semaphore(4)
    private const val IDLE_MILLIS=10*60*1000L

    class Playback internal constructor(val url:String,val filename:String,val size:Long)

    internal suspend fun register(session:AbyssSession):Playback {
        try {
            return lock.withLock {
                check(sessions.size<4) { "Close an existing Abyss playback session before opening another" }
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
                    val localClient=abyssHttpClient(15_000)
                    try {
                    val probe=localClient.head(local)
                    check(probe.status==HttpStatusCode.OK && probe.headers[HttpHeaders.ContentLength]==session.size.toString()) { "Local Abyss listener validation failed" }
                    val media=localClient.get(local) { header(HttpHeaders.Range,"bytes=0-31") }
                    check(media.status==HttpStatusCode.PartialContent) { "Local Abyss range validation failed" }
                    val bytes=media.bounded(32)
                    check(bytes.size==32 && (bytes.copyOfRange(4,8).decodeToString()=="ftyp" || bytes.take(4)==listOf(0x1a.toByte(),0x45.toByte(),0xdf.toByte(),0xa3.toByte()))) { "Abyss decrypted file is not a supported MP4/WebM/MKV video" }
                } finally { localClient.close() }
                } catch(error:Throwable) { sessions.remove(id);sessionLock.withLock { visible.remove(id) };throw error }
                Playback(local,session.filename,session.size)
            }
        } catch(error:Throwable) { withContext(NonCancellable) { session.close() };throw error }
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
        val range=AbyssRange.parse(call.request.headers[HttpHeaders.Range],session.size)
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
        val firstEnd=minOf(range.end,range.start+AbyssSession.BLOCK-1)
        val first=try { session.read(range.start,firstEnd) } catch(cancelled:kotlinx.coroutines.CancellationException) { throw cancelled }
            catch(error:AbyssUpstreamException) { return call.respond(HttpStatusCode.fromValue(error.localStatus)) }
            catch(_:Exception) { return call.respond(HttpStatusCode.BadGateway) }
        call.respondBytesWriter(contentType=session.contentType,status=status,contentLength=range.length) {
            permits.withPermit {
                writeFully(first)
                var position=firstEnd+1
                while(position<=range.end) {
                    currentCoroutineContext().ensureActive()
                    val end=minOf(range.end,position+AbyssSession.BLOCK-1)
                    writeFully(session.read(position,end));position=end+1
                }
            }
        }
    }
    // The session map is replaced/read under a separate short mutex to support the startup probe.
    private suspend fun lookup(id:String):AbyssSession?=sessionLock.withLock { visible[id] }
    private val sessionLock=Mutex()
    private val visible=mutableMapOf<String,AbyssSession>()
}
