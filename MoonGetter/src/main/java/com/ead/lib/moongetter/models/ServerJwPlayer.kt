package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

open class ServerJwPlayer(context: Context, url: String) : ServerRobot(context,url) {

    /**
     * Identifier of the server in case of throw exception
     */
    protected open val identifier : String? = null


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
    suspend fun onDefaultJwPlayer() {


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
        url = getInterceptionUrl(url,interceptionRegex,endingRegex,scriptLoader()) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,identifier.toString()))


        /**
         * Release the browser
         */
        releaseBrowser()


        /**
         * Add the default url
         */
        addDefault()
    }


    /**
     * loader script default for jwPlayer sites
     */
    protected open fun scriptLoader() : String? = """
        document.getElementsByClassName("jw-icon jw-icon-display jw-button-color jw-reset")[0].click();
    """.trimIndent()
}