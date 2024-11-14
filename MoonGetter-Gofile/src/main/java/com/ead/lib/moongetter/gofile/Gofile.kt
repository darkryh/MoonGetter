package com.ead.lib.moongetter.gofile

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Error
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.robot.ServerRobot
import okhttp3.OkHttpClient

@Pending
@Unstable(reason = "Needs to redirect  to habilitate download")
class Gofile(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerRobot(context, url, client, headers, configData) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract(): List<Video> {
        initializeBrowser()

        loadUrlAwait(url)

        evaluateJavascriptCodeAndDownload(scriptLoader())

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(context.getString(
            R.string.server_requested_resource_was_taken_down,name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        releaseBrowser()

        return listOf(
            Video(
                quality = DEFAULT,
                url = url
            )
        )
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