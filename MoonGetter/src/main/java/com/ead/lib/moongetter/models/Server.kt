package com.ead.lib.moongetter.models

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.system.extensions.onNullableResponse
import com.ead.lib.moongetter.core.system.models.MoonWebView
import com.ead.lib.moongetter.models.download.Request
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

open class Server(
    /**
     * @param @context the context of the application
     */
    protected open val context: Context,
    /**
     * @param @url the url of the server
     */
    protected open var url : String,
    /**
     * @param @isDeprecated true if the server is deprecated
     */
    open val isDeprecated : Boolean = false,
) {

    /**
     * @return true if server has videos so it means the resource was found
     */
    internal val isResourceFounded get() = videos.isNotEmpty()



    /**
     * @return true if the server is pending
     */
    internal val isPending = this::class.annotations.any { it::class == Pending::class }



    /**
     * @return list of videos direct link
     */
    val videos : List<Video> get() = _videos
    private val _videos : MutableList<Video> = mutableListOf()




    /**
     * web view that handle scraping in background for some servers
     */
    private var _webView : MoonWebView? = null
    private val webView : MoonWebView get() = _webView!!


    /**
     * representation of request results
     */
    protected val unauthorized = 401
    protected val forbidden = 403
    protected val notFound = 404
    protected val clientError = 400..499
    protected val serverError = 500..599

    private var loadedUrlListener: (String) -> Unit = {}
    private var overrideUrlListener: (String) -> Unit = {}
    private var networkInterceptorListener: (String) -> Unit = {}
    private var errorListener: (Throwable) -> Unit = {}


    /**
     * onExtract function when the solving or scraping process is getting handle
     *
     * example:
     *
     * fun onExtract() {
     *
     *   do extraction process
     *
     *   ..
     *
     *   ..
     *
     *   when finally resource is found
     *
     *   ..
     *
     *   ..
     *
     *   you can use addDefault()
     *
     *   function to add default video
     *
     *   save to the server the videos founded
     *
     *   the url has to be managed the direct  url
     *
     *   example :
     *
     *   url = "mi direct url"
     *
     *   or you can use
     *
     *   add(Video(title: String, url: String))
     *
     *   ..
     *
     *   ..
     *
     *   at final you can have a list of videos
     *
     * }
     */
    @Throws(InvalidServerException::class,IOException::class)
    open suspend fun onExtract() { /*to do in child*/}




    /**
     * add video to the list of videos
     */
    protected fun add(video: Video) { _videos.add(video) }



    /**
     * add default video to the list of videos
     * in this case the url is the direct url
     */
    protected fun addDefault() { _videos.add(Video(quality = null,  url = url, cookies = null, headers = null)) }




    /**
     * UI method to handle the instance of the web view scrapper
     */
    private fun onUi(task: () -> Unit) = com.ead.lib.moongetter.utils.Thread.onUi { task() }




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
        if ( _webView == null) return null
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
        if (_webView == null) return null

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
                Log.d("test", "shouldInterceptRequest: $url")

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
}