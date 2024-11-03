package com.ead.lib.moongetter.pixeldrain

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class Pixeldrain(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val id = PatternManager.singleMatch(
            string = url,
            regex = """/u/([a-zA-Z0-9]+)"""
        )

        url = "https://pixeldrain.com/api/file/$id?download"

        val response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = url
            )
        )
    }
}