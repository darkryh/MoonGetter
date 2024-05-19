package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server

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
        let condition = true;
    
        function loopVerifier() {
    
            setTimeout(function() {
    
                if (condition) {
    
                    condition = document.getElementsByClassName('plyr__control plyr__control--overlaid')[0] == null;
    
                    if (condition) {
    
                        loopVerifier();
    
                    }
                    else {
    
                        document.getElementsByClassName('plyr__control plyr__control--overlaid')[0].click();
                        document.getElementsByClassName('me-1 contentLink')[0].click();
    
                    }
                }
            }, 250);
        }
    
        loopVerifier();
        """.trimIndent()
}