package com.ead.project.moongetter.presentation.main.intent

import androidx.compose.ui.text.input.TextFieldValue

sealed class TextIntent : MainIntent {
    data class EnteredTargetSearch(val value: TextFieldValue) : TextIntent()
    data class ChangeSearchFocus(val isFocused: Boolean) : TextIntent()
}