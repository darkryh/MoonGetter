package com.ead.project.moongetter

import androidx.compose.ui.window.ComposeUIViewController
import com.ead.project.moongetter.app.initializeKoin

@Suppress("unused")
fun MainViewController() = run {
    initializeKoin()
    ComposeUIViewController {
        App()
    }
}