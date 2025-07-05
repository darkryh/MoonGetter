package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.toHashMap
import com.ead.lib.moongetter.utils.toHttpUrl
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Headers
import io.ktor.http.isSuccess


class Doodstream(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val hostTarget = url.toHttpUrl().host

    override suspend fun onExtract(): List<Video> {

        var response = client
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        var body = response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

        val host = response.request.url.host

        val referer = this@Doodstream.url.replace(hostTarget, host)

        val keysCode = PatternManager.singleMatch(
            string = body,
            regex =  "dsplayer\\.hotkeys[^']+'([^']+).+?function"
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        val token = PatternManager.singleMatch(
            string = body,
            regex = "makePlay.+?return[^?]+([^\"]+)"
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        val requesterUrl = "https://$host$keysCode"

        response = client.
        GET(
            requestUrl = requesterUrl,
            overrideHeaders = hashMapOf(
                "Referer" to referer
            )
        )

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        body = response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

        response = client.GET(
            overrideHeaders = requestHeaders(response.request.headers, host)
        )

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        return listOf(
            Video(
                quality = DEFAULT,
                url = (body + getRandomBuilderString() + token + (System.currentTimeMillis() / 1000L)),
                headers = response
                    .request
                    .headers
                    .toHashMap()
            )
        )
    }

    private fun getRandomBuilderString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun requestHeaders(headers: Headers, host: String) : HashMap<String,String> {
        return HashMap(
            headers.toHashMap()
                .plus("User-Agent" to  "MoonGetter")
                .plus("Referer" to "https://$host/")
        )
    }
}