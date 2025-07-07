package com.ead.lib.moongetter.onecloudfile

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.robot.ServerRobot

@Unstable("This server is not stable for waiting download process")
class OneCloudFile(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerRobot(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        initializeRobot(headers = headers)

        loadUrl(url)

        val titleState = evaluateJavascriptCode("document.title")
            .removeSurrounding("\"")

        if (titleState == "Notice ! - 1Cloud File")
            throw InvalidServerException(Resources.resourceTakenDown(name), Error.RESOURCE_TAKEN_DOWN)

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        releaseRobot()

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