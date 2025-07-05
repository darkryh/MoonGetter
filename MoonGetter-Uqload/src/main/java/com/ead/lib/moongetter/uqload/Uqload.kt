@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.uqload

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

@ExperimentalServer
class Uqload(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = url
        it["Origin"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract() : List<Video> {
        val response = client
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = PatternManager.singleMatch(
                        string = response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
                        regex = """sources:\s*\[\s*"(https?://[^"]+)"\s*\]"""
                    )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                    method = "GET",
                    headers = headers
                )
            )
        )
    }
}