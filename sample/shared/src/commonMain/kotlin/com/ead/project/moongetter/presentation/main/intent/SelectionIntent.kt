package com.ead.project.moongetter.presentation.main.intent

import com.ead.lib.moongetter.models.Request

sealed class SelectionIntent : MainIntent {
    class OnSelectedUrl(val request: Request) : SelectionIntent()
    data object Searching : SelectionIntent()
    data object MoreServerInfo : SelectionIntent()
    data object MoreVideoInfo : SelectionIntent()
    data object InfoDetailActivated : SelectionIntent()
}