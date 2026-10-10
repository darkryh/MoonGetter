package com.ead.project.moongetter.presentation.player

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import chaintech.videoplayer.host.MediaPlayerError
import androidx.compose.ui.Modifier
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import com.ead.lib.moongetter.models.Request

@Composable
actual fun Player(
    modifier: Modifier,
    request: Request
) {
    val scope = rememberCoroutineScope()
    var playbackError by remember(request) { mutableStateOf<String?>(null) }
    val playerHost = remember(request) {
        MediaPlayerHost(
            mediaUrl = request.url,
            headers = request.headers
        ).apply {
            onError = { failure ->
                println("MoonGetter desktop player failed: ${failure::class.simpleName}")
                scope.launch {
                    yield()
                    playbackError = if (failure is MediaPlayerError.VlcNotFound)
                        "Desktop playback requires VLC. Install VLC for your system architecture and restart the sample."
                    else "The desktop player could not open this video. Check the player and network configuration."
                }
            }
        }
    }

    playbackError?.let {
        Text(text = it, modifier = modifier.aspectRatio(16f / 9f))
        return
    }

    VideoPlayerComposable(
        modifier = modifier
            .aspectRatio(16f / 9f),
        playerHost = playerHost,
        playerConfig = VideoPlayerConfig()
    )
}