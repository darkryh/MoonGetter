package com.ead.project.moongetter.presentation.main.intent

import com.ead.lib.moongetter.models.Request

sealed class SelectionIntent : MainIntent {
    class OnSelectedUrl(val request: Request) : SelectionIntent()
}