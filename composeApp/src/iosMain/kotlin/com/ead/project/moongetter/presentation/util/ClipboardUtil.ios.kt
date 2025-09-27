package com.ead.project.moongetter.presentation.util

import platform.UIKit.UIPasteboard

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ClipboardUtil {
    actual fun copy(text: String, label : String) {
        UIPasteboard.generalPasteboard.string = text
    }
}