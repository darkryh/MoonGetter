package com.ead.lib.moongetter.models

import android.content.Context
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.core.system.extensions.resetClient
import com.ead.lib.moongetter.core.system.models.MoonClient
import com.ead.lib.moongetter.core.system.models.MoonWebView
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import kotlin.coroutines.resume

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
    val isResourceFounded get() = videos.isNotEmpty()



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
        onUi {
            _webView = MoonWebView(context)
            webView.settings.domStorageEnabled = domStorageEnabled
        }

        delay(200)
    }




    /**
     * Is the suspend function version of WebView.evaluateJavascript
     */
    protected suspend fun evaluateJavascriptCode(script : String) : String  = suspendCancellableCoroutine { continuation ->
        onUi {
            webView.apply {
                evaluateJavascript(script) {
                    continuation.resume(it)
                    resetClient()
                }

                continuation.invokeOnCancellation {
                    downloadDeferred.cancel()
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
                onDownloadListener = { urlResource ->
                    webView.downloadDeferred.complete(urlResource)
                }

                evaluateJavascript(script) {
                    continuation.resume(Unit)
                    resetClient()
                }

                continuation.invokeOnCancellation {
                    downloadDeferred.cancel()
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
                            if (error?.errorCode != -1 || error.errorCode != -2) return

                            continuation.cancel(Throwable("error code = ${error.errorCode}, error description =${error.description}"))
                        }
                    }

                    loadUrl(url)
                }

                continuation.invokeOnCancellation {
                    webView.downloadDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }



    /** TO-DO
     * Is the suspend function of loadingUrl but complementing by intercepting the request
     * developing the searching of the download url
     */
    protected suspend fun getInterceptionUrlAndValidateLastInterception(url: String, regex: Regex) : String?  {
        if (_webView == null) return null

        return suspendCancellableCoroutine { continuation ->
            onUi {
                //var isResultFounded = false
                webView.apply {
                    webViewClient = object : MoonClient() {

                        override fun onPageLoaded(view: WebView?, url: String?) {
                            super.onPageLoaded(view, url)

                            view?.evaluateJavascript(
                                """
                                  setTimeout(function() {
                                    document.getElementsByClassName("jw-icon jw-icon-display jw-button-color jw-reset")[0].click();
                                }, 500);  
                                """.trimIndent()
                            ) {

                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val mUrl = request?.url.toString()

                            Log.d("test", "shouldInterceptRequest: $mUrl , $regex")

                            /*if(regex.matches(mUrl) && !isResultFounded) {
                                isResultFounded = true
                                continuation.resume(mUrl)
                            }*/

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val mUrl = request?.url.toString()

                            Log.d("test", "shouldOverrideUrlLoading: $mUrl")
                            return super.shouldOverrideUrlLoading(view, request)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (error?.errorCode != -1 || error.errorCode != -2) return

                            continuation.cancel(Throwable("error code = ${error.errorCode}, error description =${error.description}"))
                        }
                    }

                    loadUrl(url)
                }

                continuation.invokeOnCancellation {
                    webView.downloadDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }


    /**
     * Is the suspend function of loadingUrl but complementing by intercepting the request
     * developing the searching of the download url
     */
    protected suspend fun getInterceptionUrl(url: String, verificationRegex: Regex, endingRegex: Regex) : String?  {
        if (_webView == null) return null

        return suspendCancellableCoroutine { continuation ->
            onUi {
                var isResultFounded = false
                webView.apply {
                    webViewClient = object : MoonClient() {

                        override fun onPageLoaded(view: WebView?, url: String?) {
                            super.onPageLoaded(view, url)

                            val response : Response? = runBlocking {
                                return@runBlocking try {
                                    OkHttpClient()
                                        .newCall(
                                            Request.Builder()
                                                .url(url?: return@runBlocking null)
                                                .build())
                                        .await()
                                } catch (e: IllegalArgumentException) {
                                    e.printStackTrace()
                                    null
                                }
                            }

                            if(response == null) {
                                isResultFounded = true
                                continuation.resume(null)
                                return
                            }

                            val code = response.code

                            if((code == forbidden || code == notFound) && !isResultFounded) {
                                isResultFounded = true
                                continuation.resume(null)
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val mUrl = request?.url.toString()

                            if(verificationRegex.matches(mUrl) && !isResultFounded) {
                                isResultFounded = true
                                continuation.resume(mUrl)
                            }

                            if(endingRegex.matches(mUrl) && !isResultFounded) {
                                isResultFounded = true
                                continuation.resume(null)
                            }

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (error?.errorCode != -1 || error.errorCode != -2) return

                            continuation.cancel(Throwable("error code = ${error.errorCode}, error description =${error.description}"))
                        }
                    }

                    loadUrl(url)
                }

                continuation.invokeOnCancellation {
                    webView.downloadDeferred.cancel()
                    releaseBrowser()
                }
            }
        }
    }




    /**
     * is the downloadableDeferredResource to get handle when is completed
     */
    protected fun downloadableDeferredResource() : CompletableDeferred<String?> {
        return webView.downloadDeferred
    }



    /**
     * Is release method from the WebView
     */
    protected open fun releaseBrowser() = onUi {
        _webView?.destroy()
        _webView = null
    }
}