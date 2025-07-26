package com.ead.project.moongetter.presentation.main.event

import com.ead.project.moongetter.presentation.util.Message

sealed class MainEvent {
     data class Notify(val message: Message) : MainEvent()
}