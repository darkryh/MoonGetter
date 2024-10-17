package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class PixelDrain(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract(): List<Video> {
        val id = PatternManager.singleMatch(
            string = url,
            regex = """/u/([a-zA-Z0-9]+)"""
        )

        url = "https://pixeldrain.com/api/file/$id?download"

        val request: Request =  Request.Builder().url(url).build()

        val response = OkHttpClient()
            .newCall(request)
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.PixelDrainIdentifier))

        return listOf(
            Video(
                quality = DEFAULT,
                url = url
            )
        )
    }
}