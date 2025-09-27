package com.ead.project.moongetter.presentation.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ClipboardUtil {
    actual fun copy(text: String, label : String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, null)
    }
}