package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class Doodstream(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val hostTarget = url.toHttpUrl().host

    override suspend fun onExtract(): List<Video> {

        var response = client
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        var body = response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)

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

        response = client.newCall(
            GET(
                url = requesterUrl,
                headers = hashMapOf(
                    "Referer" to referer
                )
            )
        ).await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        body = response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)

        response = client.newCall(
            GET(
                headers = requestHeaders(response.request.headers, host)
            )
        ).await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = (body + getRandomBuilderString() + token + (System.currentTimeMillis() / 1000L)),
                headers = response
                    .request
                    .headers
                    .toMap()
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
            headers.newBuilder()
                .add("User-Agent", "MoonGetter")
                .add("Referer", "https://$host/")
                .build()
                .toMap()
        )
    }
}