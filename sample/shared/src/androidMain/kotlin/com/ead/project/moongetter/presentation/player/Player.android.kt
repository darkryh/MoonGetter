package com.ead.project.moongetter.presentation.player

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import com.ead.lib.moongetter.models.Request

@OptIn(UnstableApi::class)
@Composable
actual fun Player(
    modifier: Modifier,
    request: Request
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    val dataSourceFactory = DataSource.Factory {
        val dataSource: HttpDataSource = httpDataSourceFactory.createDataSource()

        request.headers?.forEach { header ->
            dataSource.setRequestProperty(header.key, header.value)
        }

        dataSource
    }

    val mediaItem = MediaItem
        .Builder()
        .setUri(request.url)
        .build()


    val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)

    val startupClock = remember { longArrayOf(0L) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            addListener(
                object : Player.Listener {

                    override fun onRenderedFirstFrame() {
                        android.util.Log.d("MoonGetterPlayer", "First frame after ${android.os.SystemClock.elapsedRealtime() - startupClock[0]} ms")
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.util.Log.e("MoonGetterPlayer", "Playback failed: ${error.errorCodeName}", error)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        android.util.Log.d("MoonGetterPlayer", "State=$playbackState after ${android.os.SystemClock.elapsedRealtime() - startupClock[0]} ms")
                        when (playbackState) {
                            Player.STATE_READY -> Unit
                            Player.STATE_IDLE -> Unit
                            Player.STATE_BUFFERING -> Unit
                            Player.STATE_ENDED -> Unit
                            else -> Unit
                        }
                    }
                }
            )
            playWhenReady = true
        }
    }

    LaunchedEffect(request) {
        startupClock[0] = android.os.SystemClock.elapsedRealtime()
        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        exoPlayer.prepare()
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
                    Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                    else -> Unit
                }
            }
        )
    }
}