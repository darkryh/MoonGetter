package com.ead.lib.moongetter.core.system.models

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ead.lib.moongetter.utils.HttpUtil
import com.ead.lib.moongetter.utils.Thread

open class MoonClient(
    private val msToTimeout: Long = 10000L
) : WebViewClient() {

    private var timeout = true

    @Deprecated("OnPageInit option")
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageInit(view, url, favicon)
        timeout = true

        Thread.delay(msToTimeout) {
            if (timeout && url != HttpUtil.BLANK_BROWSER)
                onTimeout(view, url, favicon)
        }
    }

    @Deprecated("OnPageLoaded option")
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        timeout = false

        if (url != HttpUtil.BLANK_BROWSER) {
            onPageLoaded(view, url)
        }
    }

    open fun onPageInit(view: WebView?, url: String?, favicon: Bitmap?) {}
    open fun onPageLoaded(view: WebView?, url: String?) {}
    open fun onTimeout(view: WebView?, url: String?, favicon: Bitmap?) {}
}