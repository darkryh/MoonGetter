@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.mediafire

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class Mediafire(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also { it.remove("User-Agent") }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
                    regex = "(?<=href=\")(https://download\\d+\\.mediafire\\.com/[\\w\\-]+/[\\w\\-]+/[\\w\\-\\.]+)"
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
            )
        )
    }
}