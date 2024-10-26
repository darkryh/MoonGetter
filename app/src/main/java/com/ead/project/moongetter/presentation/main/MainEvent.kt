package com.ead.project.moongetter.presentation.main

import android.content.Context
import com.ead.lib.moongetter.models.Request

sealed class MainEvent {
     class OnNewResult(val context: Context,val url : String?=null) : MainEvent()
     class OnUntilFindNewResult(val context: Context,val urls: List<String>) : MainEvent()
     class OnNewResults(val context: Context,val urls : List<String> = emptyList()) : MainEvent()
     class OnSelectedUrl(val request: Request) : MainEvent()
}