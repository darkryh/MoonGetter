package com.ead.project.moongetter

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ead.lib.moongetter.mega.MegaProxy
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class MegaPlaybackTest {
    @Test fun rendersAndSeeksThroughLocalProxy()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val link=InstrumentationRegistry.getArguments().getString("megaUrl") ?: return@runBlocking
        val started=System.nanoTime()
        println("MEGA API DNS: "+java.net.InetAddress.getAllByName("g.api.mega.co.nz").joinToString { it.hostAddress.orEmpty() })
        val session=try { MegaProxy.open(link) } catch (error:Exception) {
            throw AssertionError("MEGA session initialization failed before player preparation",error)
        }
        var player:ExoPlayer?=null
        var texture:SurfaceTexture?=null
        var surface:Surface?=null
        var frame=CompletableDeferred<Unit>()
        try {
            withContext(Dispatchers.Main) {
                texture=SurfaceTexture(0)
                surface=Surface(texture)
                player=ExoPlayer.Builder(instrumentation.targetContext).build().also { p ->
                    p.volume=0f
                    p.addListener(object:Player.Listener {
                        override fun onRenderedFirstFrame() { frame.complete(Unit) }
                        override fun onPlayerError(error:PlaybackException) { frame.completeExceptionally(error) }
                    })
                    p.setVideoSurface(surface)
                    p.setMediaItem(MediaItem.fromUri(session.url));p.prepare();p.playWhenReady=true
                }
            }
            withTimeout(180_000) { frame.await() }
            println("MEGA Android initial rendered frame: ${(System.nanoTime()-started)/1_000_000} ms")
            delay(1000)
            for(target in listOf(600_000L,-1L,0L)) {
                val seekStart=System.nanoTime()
                withContext(Dispatchers.Main) {
                    frame=CompletableDeferred()
                    val p=player!!
                    assertTrue("Unknown media duration",p.duration>0)
                    p.seekTo(if(target<0) (p.duration-5000).coerceAtLeast(0) else target)
                }
                withTimeout(180_000) { frame.await() }
                delay(1000)
                println("MEGA Android rendered frame after seek: ${(System.nanoTime()-seekStart)/1_000_000} ms")
            }
        } finally {
            withContext(NonCancellable+Dispatchers.Main) { player?.release();surface?.release();texture?.release() }
            MegaProxy.release(session.url);MegaProxy.closeAll()
        }
    }
}
