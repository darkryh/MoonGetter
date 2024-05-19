package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

class Streamtape(context: Context, url : String) : Server(context,url) {

    private val interceptionRegex = """https://streamtape\.com/get_video\?id=[\w-]+&expires=\d+&ip=[\w-]+&token=[\w-]+&stream=\d+""".toRegex()
    private val endingRegex = """https://streamtape\.com/favicon\.ico""".toRegex()

    override suspend fun onExtract() {
        initializeBrowser(false)

        url = getInterceptionUrl(url,interceptionRegex,endingRegex) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.StreamtapeIdentifier))
        releaseBrowser()

        addDefault()
    }
}