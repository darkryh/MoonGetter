package com.ead.project.moongetter.presentation.util

import java.awt.Desktop

actual object IntentUtil {
    actual fun goIntentTo(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(java.net.URI(url))
        }
    }
}