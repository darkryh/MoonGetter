package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server

class Fireload(context: Context,url : String) : Server(context,url) {

    override suspend fun onExtract() {
        initializeBrowser()

        loadUrlAwait(url)
        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = downloadableDeferredResource().await() ?:"null"

        releaseBrowser()
        addDefault()
    }

    private fun scriptLoader() = """
    var verifyCondition = true;

    function verifier() {
        setTimeout(function() {
            if (verifyCondition) {
            
                verifyCondition = downloadButton.href === 'javascript:void(0)';
                
                if (verifyCondition) {
                    verifier();
                } else {
                    downloadButton.click();
                }
            }
        }, 250);
    }

    verifier();
    """.trimIndent()
}