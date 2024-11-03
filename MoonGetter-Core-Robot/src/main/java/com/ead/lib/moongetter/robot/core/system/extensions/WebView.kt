package com.ead.lib.moongetter.robot.core.system.extensions

import android.webkit.WebView
import com.ead.lib.moongetter.core.system.extensions.delete

fun WebView.getSourceCode(callback: (String) -> Unit) {
    evaluateJavascript(
        """
            let html = document.documentElement.outerHTML;
            html = html.replace(/\\n/g, '\n');
          
        """.trimIndent()
    ) { value ->

        val result = (value
            .replace("\\u003C","<")
            .lineSeparator()
            .replace("\\\"","\"")
            .takeIf { it.isNotEmpty() } ?: "null")
            .trim().removeSurrounding("\"")

        callback(result)
    }
}

fun String.toHtml() : String =
    replace("\\u003C","<")
        .lineSeparator()
        .replace("\\\"","\"")
        .takeIf { it.isNotEmpty() } ?: "null"
        .trim().removeSurrounding("\"")


fun String.lineSeparator(): String {
    return this.delete("\\n") + System.lineSeparator()
}