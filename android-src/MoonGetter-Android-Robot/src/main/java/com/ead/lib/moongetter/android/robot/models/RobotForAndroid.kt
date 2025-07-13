package com.ead.lib.moongetter.android.robot.models

import android.content.Context
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import com.ead.lib.moongetter.android.robot.core.extensions.defaultConfiguration
import com.ead.lib.moongetter.android.robot.core.model.RobotView
import com.ead.lib.moongetter.android.robot.utils.Thread.onUi
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.Server.Companion.FORBIDDEN
import com.ead.lib.moongetter.models.Server.Companion.NOT_FOUND
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.ead.lib.moongetter.client.request.Request as ClientRequest

/**
 * [RobotForAndroid] class as robot option for android devices.
 */
class RobotForAndroid(
    private val context: Context,
) : Robot {

    /**
     * Using web view as robot operations handler
     */
    private var _robotView : RobotView? = null
    private val robotView : RobotView get() = _robotView!!

    /**
     * Listener for the web view states
     */
    private var loadedUrlListener: (String) -> Unit = {}
    private var overrideUrlListener: (String) -> Unit = {}
    private var networkInterceptorListener: (String) -> Unit = {}
    private var errorListener: (Throwable) -> Unit = {}

    private var timeoutListener: (Unit) -> Unit = {}


    /**
     * Initialize the robot
     */
    override suspend fun init(domStorageEnabled : Boolean,headers : HashMap<String,String>) {
        if (_robotView != null) return

        onUi {
            _robotView = RobotView(context, headers = headers).also {
                it.defaultConfiguration(domStorageEnabled)
                it.observeStates()
            }
        }

        delay(200)
    }

    /**
     * Evaluate a javascript code in the web view
     */
    override suspend fun evaluateJavascriptCode(script: String): String = suspendCancellableCoroutine { continuation ->
        onUi {
            robotView.apply {
                evaluateJavascript(script) { continuation.resume(it) }
                continuation.invokeOnCancellation {
                    requestDeferred.cancel()
                    releaseRobotView()
                }
            }
        }
    }

    /**
     * Evaluate a javascript code in the web view and download the result
     */
    override suspend fun evaluateJavascriptCodeAndDownload(script: String) = suspendCancellableCoroutine { continuation ->
        onUi {
            robotView.apply {
                onDownloadListener = { request ->
                    robotView.requestDeferred.complete(request)
                }
                evaluateJavascript(script) {
                    continuation.resume(Unit)
                }
                continuation.invokeOnCancellation {
                    requestDeferred.cancel()
                    releaseRobotView()
                }
            }
        }
    }

    /**
     * load a url in the web view and await until completes loading
     */
    override suspend fun loadUrl(url: String) : Unit?  {
        while (_robotView == null) delay(100)

        var isLoadedOrExcepted = false

        return suspendCancellableCoroutine { continuation ->
            loadedUrlListener = {
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
                robotView.loadUrl(url)
                continuation.invokeOnCancellation {
                    robotView.requestDeferred.cancel()
                    release()
                }
            }
        }
    }

    /**
     * intercept a url in the web view and await until expected url or error url
     * in case none of them trigger await until timeout
     */
    override suspend fun getInterceptionUrl(
        url: String,
        verificationRegex: Regex,
        endingRegex: Regex,jsCode : String?,
        client: MoonClient,
        configData: Configuration.Data
    ) : String?  {
        while (_robotView == null) delay(100)

        var isResultFounded = false

        return suspendCancellableCoroutine { continuation ->

            timeoutListener(Unit)

            loadedUrlListener = { _ ->


                jsCode?.let { code ->
                    robotView.evaluateJavascript(code) {}
                }

                val response : Response = runBlocking { client.request<String>(ClientRequest(url = url, method = HttpMethod.GET)) }

                when {
                    response.isSuccess -> {
                        isResultFounded = true
                        continuation.resume(null)
                    }
                    else -> {
                        val code = response.statusCode

                        if ((code == FORBIDDEN || code == NOT_FOUND) && !isResultFounded) {
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

            timeoutListener = {
                runBlocking(Dispatchers.IO) {
                    delay(configData.timeout)
                    if (!isResultFounded) {
                        isResultFounded = true
                        continuation.resume(null)
                    }
                }
            }

            errorListener = { error -> continuation.resumeWithException(error) }

            onUi {
                robotView.loadUrl(url)
                continuation.invokeOnCancellation {
                    robotView.requestDeferred.cancel()
                    release()
                }
            }
        }
    }

    /**
     * await a download response from the robot view
     */
    override fun requestDeferredResource(): CompletableDeferred<Request?> {
        return robotView.requestDeferred
    }

    /**
     * Release the robot
     */
    override fun release() = onUi {
        _robotView?.loadUrl("about:blank")
        _robotView?.destroy()
        _robotView = null
    }


    /**
     * Observation state to known what state is the browser currently in the moment
     */
    private fun WebView.observeStates() {
        onUi {
            webViewClient = object : WebViewClient() {
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
}