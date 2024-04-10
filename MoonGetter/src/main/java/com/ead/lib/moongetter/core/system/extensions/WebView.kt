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
            ?.takeIf { it.isNotEmpty() } ?: "null")
            .trim().removeSurrounding("\"")

        callback(result)

    }
}

fun WebView.evaluateJavaScript(script: String) {
    evaluateJavascript(script, null)
}


suspend fun WebView.evaluateJavascriptSuspend(js: String): String = suspendCancellableCoroutine { continuation ->
    Thread.onUi {
        evaluateJavascript(js) { value ->
            continuation.resume(value)
        }
    }
}

fun WebView.resetClient() {
    webViewClient = WebViewClient()
}