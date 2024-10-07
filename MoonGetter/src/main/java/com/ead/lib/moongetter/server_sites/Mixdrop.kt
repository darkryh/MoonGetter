package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.models.Server

@Pending
class Mixdrop(context: Context, url: String) : Server(context, url) {
    override suspend fun onExtract() {
        //to-do
    }
}