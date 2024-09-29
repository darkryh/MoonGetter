package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class Mediafire(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {
        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.MediafireIdentifier))

        url = PatternManager.singleMatch(
            string =  response.body?.string().toString(),
            regex = "(?<=href=\")(https://download\\d+\\.mediafire\\.com/[\\w\\-]+/[\\w\\-]+/[\\w\\-\\.]+)"
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.MediafireIdentifier))

        addDefault()
    }
}