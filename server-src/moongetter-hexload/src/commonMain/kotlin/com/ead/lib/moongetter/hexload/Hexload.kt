@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.hexload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.core.system.extensions.replaceDomainWith
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.JsonObject
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import com.ead.lib.moongetter.utils.Values.targetUrl2

class Hexload(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override var url: String = targetUrl ?: url.replaceDomainWith("hexload.com")
    ?: throw InvalidServerException(Resources.invalidProcessInExpectedUrlEntry(name), Error.UNKNOWN_URL_ENTRY)

    override suspend fun onExtract(): List<Video> {
        // First request with retry
        val firstHtml = ExtractionStrategy.withRetry(
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

        // Extract data object content
        val dataContent = firstHtml.extractFirst(
            """data:\s*\{\s*(.*?)\s*\}""",
            patternFlag = RegexOption.MULTILINE
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        // Parse key-value pairs for POST body
        val postData = PatternManager.findMultipleMatchesAsPairs(
            string = dataContent,
            regex = """(\w+):\s*['"]([^'"]+)['"]"""
        ).ifEmpty { 
            throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            ) 
        }

        // Second request with retry
        val responseBody = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val response = client.POST(
                requestUrl = targetUrl2,
                overrideHeaders = hashMapOf("Content-Type" to "application/x-www-form-urlencoded"),
                body = postData
            )
            
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

        // Parse JSON response
        val videoUrl = JsonObject
            .fromJson(responseBody)
            .getJSONObject("result")
            ?.getString("url")
            ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        return listOf(Video(url = videoUrl))
    }
}