package com.ead.project.moongetter.main

import android.content.Context

sealed class MainEvent {
     class OnNewResult(val context: Context,val url : String) : MainEvent()
}