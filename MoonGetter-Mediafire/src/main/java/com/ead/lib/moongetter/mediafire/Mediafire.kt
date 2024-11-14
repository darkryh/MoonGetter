@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.mediafire

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values.targetUrl
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class Mediafire(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also { it.remove("User-Agent") }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name), Error.EMPTY_OR_NULL_RESPONSE),
                    regex = "(?<=href=\")(https://download\\d+\\.mediafire\\.com/[\\w\\-]+/[\\w\\-]+/[\\w\\-\\.]+)"
                ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name), Error.EXPECTED_RESPONSE_NOT_FOUND)
            )
        )
    }
}