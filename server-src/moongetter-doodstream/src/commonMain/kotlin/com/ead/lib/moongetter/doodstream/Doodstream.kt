package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.toHttpUrl
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class Doodstream(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val hostTarget = url.toHttpUrl().host

    override val headers: HashMap<String, String> = headers.also {
        it["Origin"] = url
        it["Referer"] = "/$url"
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun onExtract(): List<Video> {
        // First request to get initial data
        var response = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val resp = client.GET()
            
            if (!resp.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name), 
                    Error.UNSUCCESSFUL_RESPONSE, 
                    resp.statusCode
                )
            }
            
            resp
        }

        var body = response.body.asString().ifEmpty { 
            throw InvalidServerException(
                Resources.emptyOrNullResponse(name), 
                Error.EMPTY_OR_NULL_RESPONSE
            ) 
        }

        val host = response.url.host
        val referer = this@Doodstream.url.replace(hostTarget, host)

        // Extract keys code using extension method
        val keysCode = body.extractFirst(
            """dsplayer\.hotkeys[^']+'([^']+).+?function"""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        // Extract token using extension method
        val token = body.extractFirst(
            """makePlay.+?return[^?]+([^"]+)"""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        val requesterUrl = "https://$host$keysCode"

        // Second request with retry
        response = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val resp = client.GET(
                requestUrl = requesterUrl,
                overrideHeaders = hashMapOf(
                    "Referer" to referer
                )
            )
            
            if (!resp.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name), 
                    Error.UNSUCCESSFUL_RESPONSE, 
                    resp.statusCode
                )
            }
            
            resp
        }

        body = response.body.asString().ifEmpty { 
            throw InvalidServerException(
                Resources.emptyOrNullResponse(name), 
                Error.EMPTY_OR_NULL_RESPONSE
            ) 
        }

        // Third request with retry
        response = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val resp = client.GET(
                overrideHeaders = requestHeaders(response.headers, host)
            )
            
            if (!resp.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name), 
                    Error.UNSUCCESSFUL_RESPONSE, 
                    resp.statusCode
                )
            }
            
            resp
        }

        return listOf(
            Video(
                url = (body + getRandomBuilderString() + token + (Clock.System.now().toEpochMilliseconds() / 1000L)),
                headers = response.headers
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