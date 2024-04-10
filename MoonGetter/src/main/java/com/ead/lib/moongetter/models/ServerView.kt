package com.ead.lib.moongetter.models

import android.content.Context

class ServerView(
    override val context: Context,
    override var url : String
) : Server(context, url) {

    init {
        url = overridingUrl()
    }

    @Deprecated("Use on ExtractRobot Instead")
    override suspend fun onExtract() {
        initializeBrowser()
    }

    suspend fun onExtractView() {
        onExtract()
    }
}