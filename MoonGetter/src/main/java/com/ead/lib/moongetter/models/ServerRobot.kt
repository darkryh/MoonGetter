package com.ead.lib.moongetter.models

import android.content.Context
import android.os.Build
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.ead.lib.moongetter.core.system.extensions.onNullableResponse
import com.ead.lib.moongetter.core.system.models.MoonWebView
import com.ead.lib.moongetter.models.download.Request
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Server robot class for sites that needs simulates browser operations
 */
open class ServerRobot(context: Context, url : String) : Server(context,url) {


    /**
     * web view that handle scraping in background for some servers
     */
    private var _webView : MoonWebView? = null
    private val webView : MoonWebView get() = _webView!!


    /**
     * Listener for the web view states
     */
    private var loadedUrlListener: (String) -> Unit = {}
    private var overrideUrlListener: (String) -> Unit = {}
    private var networkInterceptorListener: (String) -> Unit = {}
    private var errorListener: (Throwable) -> Unit = {}

    /**
     * Initialize web view with a delay to ensure the web view is ready
     * and domStorageEnabled validation to validate depending of the server
     */
    protected suspend fun initializeBrowser(domStorageEnabled : Boolean = true)  {
        if (_webView != null) return

        onUi {
            _webView = MoonWebView(context)
            _webView?.webChromeClient = WebChromeClient()
            webView.settings.domStorageEnabled = domStorageEnabled
            configBrowser()
        }

        delay(200)
    }

    private fun configBrowser() {
        onUi {
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(
                    view: WebView?,
                    url: String?
                ) {
                    url?.let { loadedUrlListener(it) }
                    super.onPageFinished(view, url)

                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    request?.let { overrideUrlListener(it.url.toString()) }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    request?.let { networkInterceptorListener(it.url.toString()) }
                    return super.shouldInterceptRequest(view, request)
                }

                @RequiresApi(Build.VERSION_CODES.M)
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    error?.errorCode?.let {
                        if (it == -1 || it == -2) {
                            errorListener(
                                Throwable(
                                    "CODE_ERROR = ${error.errorCode}, " +
                                            "CODE_DESCRIPTION =${error.description}"
                                )
                            )
                        }
                    }
                }
            }
        }
    }



    /**
     * Is the suspend function version of WebView.evaluateJavascript
     */
    protected suspend fun evaluateJavascriptCode(script : String) : String  = suspendCancellableCoroutine { continuation ->
        onUi {
            webView.apply {
                evaluateJavascript(script) { continuation.resume(it) }
                continuation.invokeOnCancellation {
                    requestDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }




    /**
     * Is the suspend function version of WebView.evaluateJavascript and ready to download the video,
     *
     * when the WebView detects the download event,the downloadDeferred will be completed
     */
    protected suspend fun evaluateJavascriptCodeAndDownload(script : String) : Unit  = suspendCancellableCoroutine { continuation ->
        onUi {
            webView.apply {
                onDownloadListener = { request ->
                    webView.requestDeferred.complete(request)
                }
                evaluateJavascript(script) {
                    continuation.resume(Unit)
                }
                continuation.invokeOnCancellation {
                    requestDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }


    /**
     * Is the suspend function version of WebView.loadUrl
     */
    protected suspend fun loadUrlAwait(url: String) : Unit?  {
        while (_webView == null) delay(100)

        var isLoadedOrExcepted = false

        return suspendCancellableCoroutine { continuation ->
            loadedUrlListener = { url ->
                if (!isLoadedOrExcepted) {
                    isLoadedOrExcepted = true
                    continuation.resume(Unit)
                }
            }
            errorListener = { error ->
                if (!isLoadedOrExcepted) {
                    isLoadedOrExcepted = true
                    continuation.resumeWithException(error)
                }
            }
            onUi {
                webView.loadUrl(url)
                continuation.invokeOnCancellation {
                    webView.requestDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }

    /**
     * Is the suspend function of loadingUrl but complementing by intercepting the request
     * developing the searching of the download url
     */
    protected suspend fun getInterceptionUrl(url: String, verificationRegex: Regex, endingRegex: Regex,jsCode : String? = null) : String?  {
        while (_webView == null) delay(100)

        var isResultFounded = false

        return suspendCancellableCoroutine { continuation ->

            loadedUrlListener = { _ ->

                jsCode?.let { code ->
                    webView.evaluateJavascript(code) {}
                }

                val response : Response? = runBlocking { OkHttpClient().onNullableResponse(url) }
                when(response) {
                    null -> {
                        isResultFounded = true
                        continuation.resume(null)
                    }
                    else -> {
                        val code = response.code

                        if ((code == forbidden || code == notFound) && !isResultFounded) {
                            isResultFounded = true
                            continuation.resume(null)
                        }
                    }
                }
            }

            networkInterceptorListener = { url ->
                if(verificationRegex.matches(url) && !isResultFounded) {
                    isResultFounded = true
                    continuation.resume(url)
                }

                if(endingRegex.matches(url) && !isResultFounded) {
                    isResultFounded = true
                    continuation.resume(null)
                }
            }
            errorListener = { error -> continuation.resumeWithException(error) }

            onUi {
                webView.loadUrl(url)
                continuation.invokeOnCancellation {
                    webView.requestDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }

    /**
     * is the downloadableDeferredResource to get handle when is completed
     */
    protected fun requestDeferredResource() : CompletableDeferred<Request?> {
        return webView.requestDeferred
    }



    /**
     * Is release method from the WebView
     */
    protected open fun releaseBrowser() = onUi {
        _webView?.loadUrl("about:blank")
        _webView?.destroy()
        _webView = null
    }


    companion object {
        /**
         * UI method to handle the instance of the web view scrapper
         */
        private fun onUi(task: () -> Unit) = com.ead.lib.moongetter.utils.Thread.onUi { task() }
    }
}