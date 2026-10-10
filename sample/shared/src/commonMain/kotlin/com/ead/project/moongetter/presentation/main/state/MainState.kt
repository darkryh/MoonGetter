package com.ead.project.moongetter.presentation.main.state

import com.ead.project.moongetter.app.SampleConfiguration
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Video
import com.ead.project.moongetter.presentation.main.model.MainModelState
import com.ead.project.moongetter.presentation.util.TextFieldState

data class MainState(
    val targetExtractTextField : TextFieldState = TextFieldState(
        textField = androidx.compose.ui.text.input.TextFieldValue(SampleConfiguration.initialVideoUrl),
        hint = "Video URL",
        isHintVisible = false
    ),
    val streamServerName : String = "",
    val streamPlaylist: List<Video> = emptyList(),
    val selectedStream : Request? = null,
    val mainModelState: MainModelState = MainModelState.INITIALIZED,
    val shouldShowSearchingServer: Boolean = false,
    val shouldShowMoreInfoAboutSupportedServer : Boolean = false,
    val shouldShowMoreInfoAboutVideo : Boolean = false,
    val isInfoDetailActivated : Boolean = false,
    val error: String? = null
)