package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.ServerRobot
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

@Unstable(reason = "Scripts await countdown timer to load url")
class Fireload(context: Context,url : String) : ServerRobot(context,url) {

    override suspend fun onExtract(): List<Video> {
        initializeBrowser()

        loadUrlAwait(url)

        val titleState = evaluateJavascriptCode("document.title")
            .removeSurrounding("\"")

        if (titleState == "Error | Fireload")
            throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.FireloadIdentifier))

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.FireloadIdentifier))

        releaseBrowser()

        return listOf(
            Video(
                quality = DEFAULT,
                url = url
            )
        )
    }

    private fun scriptLoader() = """
    function verifier() {
        setTimeout(function() {
            if (downloadButton.href === 'javascript:void(0)') {
                verifier();
            } else {
                downloadButton.click();
            }
        }, 250);
    }
    verifier();
    """.trimIndent()
}