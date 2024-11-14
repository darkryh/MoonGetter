package com.ead.project.moongetter.presentation.main.intent

import android.content.Context

sealed class NetworkIntent : MainIntent {
    class OnGetResult(val context: Context, val url: String? = null) : NetworkIntent()
    class OnGetUntilFindNewResult(val context: Context, val urls: List<String>) : NetworkIntent()
    class OnGetResults(val context: Context, val urls: List<String> = emptyList()) : NetworkIntent()
}