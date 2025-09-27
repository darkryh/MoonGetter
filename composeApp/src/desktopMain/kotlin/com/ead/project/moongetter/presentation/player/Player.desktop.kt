package com.ead.project.moongetter.presentation.player

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerEvent
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.ead.lib.moongetter.models.Request
import kotlinx.coroutines.delay

@Composable
actual fun Player(
    modifier: Modifier,
    request: Request
) {
    val playerHost = remember {
        MediaPlayerHost(
            mediaUrl = request.url,
            headers = request.headers
        )
    }

    LaunchedEffect(Unit) {
        delay(15000L)
        playerHost.onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused = true))
    }

    VideoPlayerComposable(
        modifier = modifier
            .aspectRatio(16f / 9f),
        playerHost = playerHost,
        playerConfig = VideoPlayerConfig()
    )
}