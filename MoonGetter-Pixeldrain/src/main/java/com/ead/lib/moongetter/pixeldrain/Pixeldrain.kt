package com.ead.lib.moongetter.pixeldrain

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class Pixeldrain(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val id = PatternManager.singleMatch(
            string = url,
            regex = """/u/([a-zA-Z0-9]+)"""
        )

        url = "https://pixeldrain.com/api/file/$id?download"

        val response = client
            .configBuilder()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = url
            )
        )
    }
}