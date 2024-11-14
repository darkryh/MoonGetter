package com.ead.lib.moongetter.onecloudfile

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Error
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.robot.ServerRobot
import okhttp3.OkHttpClient

@Unstable("This server is not stable for waiting download process")
class OneCloudFile(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerRobot(context, url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        initializeBrowser()

        loadUrlAwait(url)

        val titleState = evaluateJavascriptCode("document.title")
            .removeSurrounding("\"")

        if (titleState == "Notice ! - 1Cloud File")
            throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name), Error.RESOURCE_TAKEN_DOWN)

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(context.getString(
            R.string.server_requested_resource_was_taken_down, name), Error.EXPECTED_RESPONSE_NOT_FOUND)

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