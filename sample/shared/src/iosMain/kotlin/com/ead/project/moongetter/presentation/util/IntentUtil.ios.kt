package com.ead.project.moongetter.presentation.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual object IntentUtil {
    actual fun goIntentTo(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return

        UIApplication.sharedApplication.openURL(
            nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = { success -> println("Open URL success: $success") }
        )
    }
}