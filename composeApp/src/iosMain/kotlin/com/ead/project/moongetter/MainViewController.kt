package com.ead.project.moongetter

import androidx.compose.ui.window.ComposeUIViewController
import com.ead.project.moongetter.app.initializeKoin

fun MainViewController() = run {
    initializeKoin()
    
    ComposeUIViewController {
        App()
    }
}
