package com.ead.lib.moongetter.yourupload

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class YourUpload(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string =  response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
                    regex = """file:\s*'([^']*)'""",
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                headers = hashMapOf(
                    "Referer" to "https://www.yourupload.com/"
                )
            )
        )
    }
}