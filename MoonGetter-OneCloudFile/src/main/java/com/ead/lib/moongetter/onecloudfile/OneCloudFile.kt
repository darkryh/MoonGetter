package com.ead.lib.moongetter.onecloudfile

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.ServerRobot
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

@Unstable("This server is not stable for waiting download process")
class OneCloudFile(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : ServerRobot(context,url,headers) {

    override suspend fun onExtract(): List<Video> {
        initializeBrowser()

        loadUrlAwait(url)

        val titleState = evaluateJavascriptCode("document.title")
            .removeSurrounding("\"")

        if (titleState == "Notice ! - 1Cloud File")
            throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(context.getString(
            R.string.server_requested_resource_was_taken_down, name))

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