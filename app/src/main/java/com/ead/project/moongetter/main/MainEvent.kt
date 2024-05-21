package com.ead.project.moongetter.main

import android.content.Context

sealed class MainEvent {
     class OnNewResult(val context: Context,val url : String?=null) : MainEvent()
     class OnUntilFindNewResult(val context: Context,val urls: List<String>) : MainEvent()
     class OnNewResults(val context: Context,val urls : List<String> = emptyList()) : MainEvent()
     class OnSelectedUrl(val url : String) : MainEvent()
}