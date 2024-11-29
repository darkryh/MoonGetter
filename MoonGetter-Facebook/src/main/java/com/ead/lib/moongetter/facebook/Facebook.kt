@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.facebook

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import okhttp3.FormBody
import okhttp3.OkHttpClient

class Facebook(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    init {
        targetUrl = targetUrl ?: "https://fdown.net/download.php"
    }

    override suspend fun onExtract() : List<Video> {
        val response = client
            .configBuilder()
            .newCall(
                POST(
                    url = targetUrl,
                    formBody = FormBody.Builder()
                        .add("URLz", url)
                        .build()
                )
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        val body = response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)

        return listOf(
            Video(
                quality = DEFAULT,
                url = (PatternManager.singleMatch(
                    string = body,
                    regex = "id=\"sdlink\".*?href=\"(.*?)\""
                ) ?: PatternManager.singleMatch(
                    string = body,
                    regex = "id=\"hdlink\".*?href=\"(.*?)\""
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)).replace("&amp;", "&")
            )
        )
    }
}