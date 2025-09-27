package com.ead.project.moongetter.presentation.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.ead.project.moongetter.MoonGetterApp

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ClipboardUtil {
    actual fun copy(text: String,label : String) {
        val clipboard = MoonGetterApp.instance.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
}