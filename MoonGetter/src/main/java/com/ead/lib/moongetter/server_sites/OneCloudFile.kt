package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

class OneCloudFile(context: Context,url: String) : Server(context,url) {

    override suspend fun onExtract() {
        initializeBrowser()

        loadUrlAwait(url)

        val titleState = evaluateJavascriptCode("document.title")
            .removeSurrounding("\"")

        if (titleState == "Notice ! - 1Cloud File")
            throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.OneCloudFileIdentifier))

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = downloadableDeferredResource().await() ?:"null"

        releaseBrowser()
        addDefault()
    }

    private fun scriptLoader() = """
    function verifier() {
        setTimeout(function() {
            var downloadButton = document.querySelector(".uk-button.uk-button-secondary.uk-text-truncate.uk-width-1-1");

            if (downloadButton && downloadButton.textContent.includes("DOWNLOAD FILE")) {
                downloadButton.click();
            } else {
                verifier();
            }
        }, 250);
    }

    verifier();
    """.trimIndent()
}