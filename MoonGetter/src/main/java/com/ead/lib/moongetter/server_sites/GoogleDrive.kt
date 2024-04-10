package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server

class GoogleDrive(context: Context, url : String) : Server(context,url) {

    override fun overridingUrl(): String = fixUrl(url)

    override suspend fun onExtract() {
        initializeBrowser()

        loadUrlAwait(url)

        evaluateJavascriptCodeAndDownload(scriptLoader())
        releaseBrowser()
        addDefault()
    }


    private fun fixUrl(url : String) = "https://drive.google.com/u/0/uc?id=${getFileId(url)}&export=download"

    private fun getFileId(string: String) = string.substringAfter("/d/")
        .substringBefore("/preview")
        .substringBefore("/view")

    private fun scriptLoader() = "document.getElementById('uc-download-link').click();"
}