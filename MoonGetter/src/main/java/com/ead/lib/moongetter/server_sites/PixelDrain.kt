package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.Server

class PixelDrain(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {

        val regexPattern = Regex("""/u/([a-zA-Z0-9]+)""")
        val matchResult = regexPattern.find(url)
        val id = matchResult?.groupValues?.get(1)

        url = "https://pixeldrain.com/api/file/$id?download"
    }
}