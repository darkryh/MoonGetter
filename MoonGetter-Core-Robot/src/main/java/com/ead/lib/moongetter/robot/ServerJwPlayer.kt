package com.ead.lib.moongetter.robot

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Error
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okhttp3.OkHttpClient

open class ServerJwPlayer(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerRobot(context, url, client, headers, configData) {


    /**
     * Regex to find the url of the video
     * intercepted
     */
    protected open val interceptionRegex = ".*master\\.m3u8.*".toRegex()


    /**
     * Regex to find the url of the video
     * in case the interception fails
     */
    protected open val endingRegex = """.*/randomStr$""".toRegex()


    /**
     * Default method to extract the url of the video for jwPlayers sites
     */
    suspend fun onDefaultJwPlayer() : List<Video> {


        /**
         * Initialize the browser
         */
        initializeBrowser(false)


        /**
         * Extract the url of the video
         * by intercepting the request
         * first load the jwPlayer by script
         * and then intercept the expected url
         */
        url = getInterceptionUrl(
            url = url,
            verificationRegex = interceptionRegex,
            endingRegex = endingRegex,
            jsCode = scriptLoader()
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        /**
         * Release the browser
         */
        releaseBrowser()


        /**
         * Add the default url
         */
        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = url,
                    method = "GET",
                    headers = headers
                )
            )
        )
    }


    /**
     * loader script default for jwPlayer sites
     */
    protected open fun scriptLoader() : String? = """
        document.getElementsByClassName("jw-icon jw-icon-display jw-button-color jw-reset")[0].click();
    """.trimIndent()
}