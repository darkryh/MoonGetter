package com.ead.lib.moongetter.yourupload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractWithFallback
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.CommonPatterns
import com.ead.lib.moongetter.utils.ExtractionStrategy

class YourUpload(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
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

        // Try multiple pattern strategies with fallback
        val videoUrl = html.extractWithFallback(
            // Primary pattern - most common (single quotes)
            """file:\s*'([^']*)'""",
            // Fallback pattern 1 - double quotes
            """file:\s*"([^"]*)"""",
            // Fallback pattern 2 - without quotes
            """file:\s*([^\s,]+)""",
            // Fallback pattern 3 - from source tag
            CommonPatterns.Video.SOURCE_TAG
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        return listOf(
            Video(
                url = videoUrl,
                headers = hashMapOf(
                    "Referer" to "https://www.yourupload.com/"
                )
            )
        )
    }
}