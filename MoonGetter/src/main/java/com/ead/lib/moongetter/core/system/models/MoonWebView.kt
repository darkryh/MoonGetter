package com.ead.lib.moongetter.core.system.models

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import com.ead.lib.moongetter.models.download.Request
import kotlinx.coroutines.CompletableDeferred

@SuppressLint("SetJavaScriptEnabled")
class MoonWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?= null,
    defStyle : Int=0,
    defStylerRes: Int=0) : WebView(context,attrs,defStyle,defStylerRes) {

    var onDownloadListener: (Request) -> Unit = {}
    val requestDeferred = CompletableDeferred<Request?>(null)

    init {
        settings.apply {
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
            javaScriptEnabled = true
        }

        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            onDownloadListener(
                Request(
                    url = url,
                    method = "GET",
                    headers = mapOf(
                        "User-Agent" to userAgent,
                        "Content-Disposition" to contentDisposition,
                        "Content-Type" to mimetype,
                        "Content-Length" to contentLength.toString()
                    ),
                )
            )
        }
    }

    fun releaseBrowser() {
        requestDeferred.cancel()
    }
}