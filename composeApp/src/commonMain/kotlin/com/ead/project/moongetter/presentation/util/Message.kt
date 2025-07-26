package com.ead.project.moongetter.presentation.util

sealed class Message(open val data: String) {
    data class Information(override val data: String) : Message(data)
    data class Warning(override val data: String) : Message(data)
    data class Error(override val data: String) : Message(data)
}