package com.ead.project.moongetter.presentation.main.intent

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import com.ead.lib.moongetter.models.Request


sealed  class MainIntent {
    data class EnteredTargetSearch(val value: TextFieldValue): MainIntent()
    class OnGetResult(val context: Context,val url : String?=null) : MainIntent()
    class OnGetUntilFindNewResult(val context: Context,val urls: List<String>) : MainIntent()
    class OnGetResults(val context: Context,val urls : List<String> = emptyList()) : MainIntent()
    class OnSelectedUrl(val request: Request) : MainIntent()
}