package com.ead.lib.moongetter.models

import android.content.Context
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.getSourceCode
import com.ead.lib.moongetter.core.system.extensions.resetClient
import com.ead.lib.moongetter.core.system.models.MoonClient
import com.ead.lib.moongetter.core.system.models.MoonWebView
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.IOException
import kotlin.coroutines.resume

open class Server(
    open val context: Context,
    open var url : String
) {

    protected var isResourceFounded = false

    val files : List<File> get() = _files
    protected val _files : MutableList<File> = mutableListOf()

    val domains : List<String> get() = _domains
    protected val _domains : MutableList<String> = mutableListOf()

    private var _webView : MoonWebView? = null
    private val webView : MoonWebView get() = _webView!!

    init {
        url = overridingUrl()
    }

    open fun overridingUrl() : String {
        return url
    }

    @Throws(InvalidServerException::class,IOException::class)
    open suspend fun onExtract() { /*to do in child*/}

    protected fun add(file: File) { _files.add(file) }
    protected fun addDefault() { _files.add(File(Properties.Default,url)) }

    protected fun onUi(task: () -> Unit) = com.ead.lib.moongetter.utils.Thread.onUi { task() }

    protected suspend fun initializeBrowser()  {
        onUi {
            _webView = MoonWebView(context)
        }

        delay(200)
    }


    suspend fun evaluateJavascriptCodeAndDownload(script : String) : Unit  = suspendCancellableCoroutine { continuation ->
        onUi {
            webView.apply {
                evaluateJavascript(script) {
                    continuation.resume(Unit)
                }

                onDownloadListener = { urlResource ->
                    webView.downloadDeferred.complete(urlResource)
                }

                continuation.invokeOnCancellation {
                    downloadDeferred.cancel()
                }
            }
        }
        while (!isResourceFounded) print("holding")
        continuation.resume(Unit)
    }

    protected suspend fun loadUrlAwait(url: String) : Unit?  {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || _webView == null) return null

        return suspendCancellableCoroutine { continuation ->
            onUi {

                webView.apply {
                    webViewClient = object : MoonClient() {

                        override fun onPageLoaded(view: WebView?, url: String?) {
                            super.onPageLoaded(view, url)
                            continuation.resume(Unit)
                            resetClient()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            continuation.cancel(Throwable("error ${error?.errorCode}, ${error?.description}"))
                        }
                    }

                    loadUrl(url)
                }
            }
        }
    }

    protected suspend fun getSourceCodeFrom(url: String) : String?  {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || _webView == null) return null

        return suspendCancellableCoroutine { continuation ->
            onUi {
                var isResultFounded = false
                webView.apply {
                    webViewClient = object : MoonClient() {

                        override fun onPageLoaded(view: WebView?, url: String?) {
                            super.onPageLoaded(view, url)
                            webView.getSourceCode {
                                if (!isResultFounded)
                                    continuation.resume(it)
                                isResultFounded = true
                            }
                        }
                    }
                    /*onLoaded = { _, _ ->
                        webView.getSourceCode {
                            continuation.resume(it)
                        }
                    }
                    onError = { _, _, _ ->
                        *//*if (errorCode != -1  || description != "net::ERR_FAILED") {
                            continuation.cancel(Throwable("Error loading URL: $errorCode, $description"))
                        }*//*
                    }*/

                    loadUrl(url)
                }
            }
        }
    }

    protected fun downloadableDeferredResource() : CompletableDeferred<String?> {
        return webView.downloadDeferred
    }

    protected open fun releaseBrowser() = onUi {
        _webView?.destroy()
        _webView = null
    }
}