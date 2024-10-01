package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Server

@Pending
@Unstable(reason = "Needs to redirect  to habilitate download")
class Gofile(context: Context, url : String) : Server(context,url) {

    override val isDeprecated: Boolean get() = true

    override suspend fun onExtract() {
        initializeBrowser()

        loadUrlAwait(url)
        evaluateJavascriptCodeAndDownload(scriptLoader())
        url = downloadableDeferredResource().await() ?:"null"

        releaseBrowser()
        addDefault()
    }

    private fun scriptLoader() = """
    function loopVerifier() {
        setTimeout(function() {
            const overlaidButton = document.querySelector('.plyr__control.plyr__control--overlaid');
            const contentLink = document.querySelector('.me-1.contentLink');
            
            if (!overlaidButton) {
                loopVerifier();
            } else {
                overlaidButton.click();
                if (contentLink) {
                    contentLink.click();
                }
            }
        }, 250);
    }

    loopVerifier();
        """.trimIndent()
}