package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

@Pending
class Vidguard(context: Context,url: String) : Server(context,url){

    //private val  interceptionRegex = """https://[a-zA-Z0-9]+\.[a-zA-Z0-9]+\.[a-zA-Z0-9]+/hls_3/[a-zA-Z0-9_\-]+/master\.m3u8\?sig=[a-zA-Z0-9_\-]+&expires=\d+"""".toRegex()

    override val isDeprecated: Boolean get() = false

    override suspend fun onExtract() {
        initializeBrowser()

        url = getInterceptionUrlAndValidateLastInterception(url,Properties.Vidguard.toRegex()) ?:throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.VidguardIdentifier))

        releaseBrowser()

        addDefault()
    }
}