package com.ead.lib.moongetter.server_sites

import android.content.Context
import android.webkit.WebView
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.core.system.extensions.evaluateJavaScript
import com.ead.lib.moongetter.core.system.models.MoonWebView
import com.ead.lib.moongetter.core.system.models.ServerWebClient
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class StreamSb(context: Context, url : String) : Server(context,url) {

    private val rawServers : MutableList<String> = mutableListOf()

    override suspend fun onExtract() {

        url = fixUrl(url)

        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        val host = response.request.url.host

        val totalData: List<String> = PatternManager.multipleMatches(
            string =  response.body?.string().toString(),
            regex =  "onclick=\"download_video(.*?)\""
        )

        if (totalData.isEmpty()) throw InvalidServerException("StreamSb resource couldn't find it")

        for (data in totalData) {

            val dividedData = fixPreviewUrl(data)
            val fileId = dividedData[0]
            val fileMode = dividedData[1]
            val fileHash = dividedData[2]
            rawServers.add( "https://" + host + "/dl?op=download_orig" +
                    "&id=" + fileId +
                    "&mode=" + fileMode +
                    "&hash=" + fileHash )
        }

        url = rawServers.first()

    }

    private fun initWebView() = onUi {
        /*_webView = MoonWebView(context)

        _webView?.webViewClient = object : ServerWebClient(webView = _webView) {
            override fun onPageLoaded(view: WebView?, url: String?) {
                super.onPageLoaded(view, url)
                view?.apply {

                    when(timesLoaded) {
                        1 -> { evaluateJavaScript(skipCaptcha()) }
                        2 -> {
                            evaluateJavascript(getDownloadScript()) { data ->
                                this@StreamSb.url = data.removePrefix("\"").removeSuffix("\"")
                                //_webView?.isLoading = false
                            }
                        }
                    }

                }
            }
        }
        setupWebView()*/
    }

    private fun skipCaptcha() = "document.getElementsByClassName('g-recaptcha')[0].click();"

    private fun getDownloadScript() =
        "let data = document.getElementsByClassName('btn btn-light btn-lg d-inline-flex align-items-center justify-content-center py-3 px-5')[0]; " +
                "(function() { return data.getAttribute('href'); })(); "

    private fun fixUrl(url : String) : String {
        if (url.contains("/e/"))
            return url.replace("/e/","/d/")

        if (url.contains("/d/"))
            return url

        val regex = Regex("(https?://[^/]+)(/.*)")

        return regex.replace(url) { result ->
            val (host, path) = result.destructured
            "$host/d$path"
        }
    }

    private fun fixPreviewUrl(url : String) = url.removePrefix("(").removeSuffix(")")
        .replace("'","").split(",")
}