package com.ead.lib.moongetter.core.system.extensions

import android.webkit.WebView
import android.webkit.WebViewClient
import com.ead.lib.moongetter.utils.Thread
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

fun String.lineSeparator(): String {
    return this.delete("\\n") + System.lineSeparator()
}


fun WebView.resetClient() {
    webViewClient = WebViewClient()
}