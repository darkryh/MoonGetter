package com.ead.project.moongetter.presentation.main.state

import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Video
import com.ead.project.moongetter.presentation.util.TextFieldState

data class MainState(
    val targetExtractTextField : TextFieldState = TextFieldState(
        hint = "set url to stream",
        isHintVisible = true
    ),
    val streamPlaylist: List<Video> = emptyList(),
    val selectedStream : Request? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)