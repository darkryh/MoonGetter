package com.ead.lib.moongetter.server_sites.util

import android.content.Context
import com.ead.lib.moongetter.models.Server

class UnknownServer(context: Context, url : String) : Server(context,url) {
    override suspend fun onExtract() {
        url = "null"
        addDefault()
    }
}