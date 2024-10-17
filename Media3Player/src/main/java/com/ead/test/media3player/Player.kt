package com.ead.test.media3player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.download.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@Composable
fun Player(
    modifier: Modifier = Modifier,
    request: Request?,
) {

    if (request == null) return

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val dataSourceFactory = DataSource.Factory {
        DefaultHttpDataSource.Factory().apply {
            request.headers?.forEach { (key, value) ->
                setDefaultRequestProperties(mapOf(key to value))
            }
        }.createDataSource()
    }

    val mediaItem = MediaItem
        .Builder()
        .setUri(request.url)
        .build()

    val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
        .createMediaSource(mediaItem)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            setMediaSource(mediaSource)
            addListener(
                object : Player.Listener {

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                runBlocking { performPreRequest(request) }
                            }
                            Player.STATE_IDLE -> {

                            }
                            Player.STATE_BUFFERING -> {

                            }
                            Player.STATE_ENDED -> {

                            }
                            else ->{}
                        }
                    }


                }
            )
            playWhenReady = true
            prepare()
        }
    }

    var lifecycle by remember {
        mutableStateOf(Lifecycle.Event.ON_CREATE)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> lifecycle = event }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Column(modifier = modifier.background(Color.Black)) {
        AndroidView(
            modifier = modifier.aspectRatio(16 / 9f),
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setShowFastForwardButton(false)
                    setShowRewindButton(false)
                }
            },
            update = {
                when (lifecycle) {
                    Lifecycle.Event.ON_START -> exoPlayer.playWhenReady = true
                    Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> {
                        exoPlayer.play()
                        exoPlayer.setMediaItem(mediaItem)
                    }
                    else -> Unit
                }
            }
        )
    }
}


suspend fun performPreRequest(request: Request): String? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        val request = okhttp3.Request.Builder()
            .url("https://uqload.ws/dl?op=view&file_code=px8mfh4wqk43&hash=2166554-56-19-1729037363-0814795e7f7a0c5c29c50e075c8de846&embed=1&adb=0")
            .headers(
                Headers.Builder().also {
                    request.headers?.forEach { (key, value) ->
                        it.add(key, value)
                    }
                    it
                }
                    .build()
            )
            .build()

        val response = client.newCall(request).await()

        response.code.toString()
    }
}