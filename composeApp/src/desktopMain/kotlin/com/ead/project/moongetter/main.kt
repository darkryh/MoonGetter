package com.ead.project.moongetter

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ead.project.moongetter.app.initializeKoin

fun main() = application {
    initializeKoin()
    Window(
        onCloseRequest = ::exitApplication,
        title = "MoonGetter",
    ) { App() }
}