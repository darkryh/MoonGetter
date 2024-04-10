package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server

class Gofile(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {
        initializeBrowser()

        loadUrlAwait(url)
        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = downloadableDeferredResource()?.await() ?:"null"

        releaseBrowser()
        addDefault()
    }

    private fun scriptLoader() = "document.querySelector('.btn.btn-outline-secondary.btn-sm.p-1.text-white').click();"
}