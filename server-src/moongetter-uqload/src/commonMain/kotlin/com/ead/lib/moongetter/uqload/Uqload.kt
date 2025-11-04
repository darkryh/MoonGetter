@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.uqload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractWithFallback
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.Values.targetUrl

@ExperimentalServer
class Uqload(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = url
        it["Origin"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract() : List<Video> {
        // Use retry mechanism for resilient extraction
        val html = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val response = client.GET()
            
            if (!response.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name), 
                    Error.UNSUCCESSFUL_RESPONSE, 
                    response.statusCode
                )
            }
            
            response.body.asString().ifEmpty { 
                throw InvalidServerException(
                    Resources.emptyOrNullResponse(name), 
                    Error.EMPTY_OR_NULL_RESPONSE
                ) 
            }
        }

        // Try multiple pattern strategies with validation
        val videoUrl = ExtractionStrategy.withValidation(
            config = ExtractionStrategy.RetryConfig.NoRetry,
            validator = { it.startsWith("http") }
        ) {
            html.extractWithFallback(
                // Primary pattern - sources array with double quotes
                """sources:\s*\[\s*"(https?://[^"]+)"\s*\]""",
                // Fallback - sources array with single quotes
                """sources:\s*\[\s*'(https?://[^']+)'\s*\]""",
                // Fallback - source without array
                """source:\s*"(https?://[^"]+)"""",
                // Fallback - file property
                """file:\s*"(https?://[^"]+)""""
            ) ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )
        }

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = videoUrl,
                    method = "GET",
                    headers = headers
                )
            )
        )
    }
}