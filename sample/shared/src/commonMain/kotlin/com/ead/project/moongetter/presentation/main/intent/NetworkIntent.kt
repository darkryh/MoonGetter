package com.ead.project.moongetter.presentation.main.intent


sealed class NetworkIntent : MainIntent {
    class OnGetResult(val url: String? = null) : NetworkIntent()
    class OnGetUntilFindNewResult(val urls: List<String>) : NetworkIntent()
    class OnGetResults(val urls: List<String> = emptyList()) : NetworkIntent()
}