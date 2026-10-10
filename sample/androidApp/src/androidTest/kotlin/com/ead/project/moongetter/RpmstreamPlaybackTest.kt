package com.ead.project.moongetter

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ead.lib.moongetter.rpmstream.factory.RpmstreamFactory
import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.engine.okhttp.OkHttp
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class RpmstreamPlaybackTest {
    @Test fun rendersAndSeeksNativeHls()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val link=InstrumentationRegistry.getArguments().getString("rpmUrl") ?: return@runBlocking
        val started=System.nanoTime()
        val config=Configuration.Data(timeout=60_000,disableSSLValidation=false)
        val client=KtorMoonClient(OkHttp,MoonCookie.Management.newEmptyFactory(),MoonTrust.Manager.newEmptyFactory())
        client.initConfigurationData(config)
        val request=RpmstreamFactory.create(link,hashMapOf("User-Agent" to "Mozilla/5.0"),config,client).onExtract().single().request
        var player:ExoPlayer?=null
        var texture:SurfaceTexture?=null
        var surface:Surface?=null
        var frame=CompletableDeferred<Unit>()
        try {
            withContext(Dispatchers.Main) {
                texture=SurfaceTexture(0)
                surface=Surface(texture)
                player=ExoPlayer.Builder(instrumentation.targetContext).setMediaSourceFactory(DefaultMediaSourceFactory(DefaultHttpDataSource.Factory().setDefaultRequestProperties(request.headers.orEmpty()))).build().also { p ->
                    p.volume=0f
                    p.addListener(object:Player.Listener {
                        override fun onRenderedFirstFrame() { frame.complete(Unit) }
                        override fun onPlayerError(error:PlaybackException) { frame.completeExceptionally(error) }
                    })
                    p.setVideoSurface(surface)
                    p.setMediaItem(MediaItem.fromUri(request.url));p.prepare();p.playWhenReady=true
                }
            }
            withTimeout(180_000) { frame.await() }
            println("RPMStream Android initial rendered frame: ${(System.nanoTime()-started)/1_000_000} ms")
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
                println("RPMStream Android rendered frame after seek: ${(System.nanoTime()-seekStart)/1_000_000} ms")
            }
        } finally {
            withContext(NonCancellable+Dispatchers.Main) { player?.release();surface?.release();texture?.release() }
        }
    }
}
