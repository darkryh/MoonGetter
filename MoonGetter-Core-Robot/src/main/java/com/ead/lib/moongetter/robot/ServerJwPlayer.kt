@file:Suppress("unused")

package com.ead.lib.moongetter.robot

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.client.HttpClient

open class ServerJwPlayer(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerRobot(url, client, headers, configData) {


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
        initializeRobot(domStorageEnabled = false, headers = headers)


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
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        /**
         * Release the browser
         */
        releaseRobot()


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