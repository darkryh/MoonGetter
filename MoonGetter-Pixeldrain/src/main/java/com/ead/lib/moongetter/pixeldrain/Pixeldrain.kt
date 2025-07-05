package com.ead.lib.moongetter.pixeldrain

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import io.ktor.client.HttpClient
import io.ktor.http.isSuccess

class Pixeldrain(
    url : String,
    client: HttpClient,
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
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        return listOf(
            Video(
                quality = DEFAULT,
                url = url
            )
        )
    }
}