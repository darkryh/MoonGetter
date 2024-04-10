package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server

class Mediafire(context: Context, url : String) : Server(context,url) {

    private val downloadRegex =
        Regex("""https?://(?:www\.)?mediafire\.com/download/.+""")

    override suspend fun onExtract() {
        initializeBrowser()

        loadUrlAwait(url)
        evaluateJavascriptCodeAndDownload(loadedScript())

        url = downloadableDeferredResource().await() ?: "null"

        releaseBrowser()
        addDefault()
    }


    private fun loadedScript() =
        "document.getElementById('downloadButton').click();"
}