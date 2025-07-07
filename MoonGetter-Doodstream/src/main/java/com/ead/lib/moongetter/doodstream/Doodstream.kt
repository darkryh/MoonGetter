package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.toHttpUrl


class Doodstream(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val hostTarget = url.toHttpUrl().host

    override suspend fun onExtract(): List<Video> {

        var response = client
            .GET()

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        var body = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

        val host = response.url.host

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

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        body = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

        response = client.GET(
            overrideHeaders = requestHeaders(response.headers, host)
        )

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        return listOf(
            Video(
                quality = DEFAULT,
                url = (body + getRandomBuilderString() + token + (System.currentTimeMillis() / 1000L)),
                headers = response
                    .headers
            )
        )
    }

    private fun getRandomBuilderString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun requestHeaders(headers: Map <String, String>, host: String) : HashMap<String,String> {
        return HashMap(
            headers
                .plus("User-Agent" to  "MoonGetter")
                .plus("Referer" to "https://$host/")
        )
    }
}