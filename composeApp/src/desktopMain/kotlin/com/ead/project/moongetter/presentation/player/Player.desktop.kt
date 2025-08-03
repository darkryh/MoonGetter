package com.ead.project.moongetter.presentation.player

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.ead.lib.moongetter.models.Request

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

    VideoPlayerComposable(
        modifier = modifier
            .aspectRatio(16f / 9f),
        playerHost = playerHost
    )
}