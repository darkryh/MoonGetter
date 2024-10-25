package com.ead.lib.moongetter.mediafire

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class Mediafire(
    context: Context,
    url : String,
    headers : HashMap<String,String>
) : Server(context,url,headers) {

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
                    regex = "(?<=href=\")(https://download\\d+\\.mediafire\\.com/[\\w\\-]+/[\\w\\-]+/[\\w\\-\\.]+)"
                ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))
            )
        )
    }
}