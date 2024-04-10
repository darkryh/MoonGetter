package com.ead.lib.moongetter.core.system.models

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred

@SuppressLint("SetJavaScriptEnabled")
class MoonWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?= null,
    defStyle : Int=0,
    defStylerRes: Int=0) : WebView(context,attrs,defStyle,defStylerRes) {

    val downloadDeferred = CompletableDeferred<String?>(null)

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = "Mozilla/5.0 (Android 4.4; Mobile; rv:41.0) Gecko/41.0 Firefox/41.0"
        }

        setDownloadListener { url, _, _, _, _ ->
            onDownloadListener(url)
        }
    }

    var onDownloadListener: (String) -> Unit = {}
}