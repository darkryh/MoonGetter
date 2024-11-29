package com.ead.lib.moongetter.fireload

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.robot.ServerRobot
import okhttp3.OkHttpClient

@Unstable(reason = "Scripts await countdown timer to load url")
class Fireload(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerRobot(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        initializeRobot(headers = headers)

        loadUrl(url)

        val titleState = evaluateJavascriptCode("document.title")
            .removeSurrounding("\"")

        if (titleState == "Error | Fireload")
            throw InvalidServerException(Resources.resourceTakenDown(name), Error.RESOURCE_TAKEN_DOWN)

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(Resources.resourceTakenDown(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

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