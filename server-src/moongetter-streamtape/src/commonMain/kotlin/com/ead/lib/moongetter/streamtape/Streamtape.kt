package com.ead.lib.moongetter.streamtape

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy

class Streamtape(
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

        // Extract video URL using the new utility methods
        val urlPart = html.extractFirst(
            """div id="robotlink" style="display:none;">(.*?token=)[^&]*"""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )
        
        val token = html.extractFirst(
            """document\.getElementById\('robotlink'\)\.innerHTML\s*=\s*.*?token=([\w-]+)"""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        return listOf(
            Video(url = "https:$urlPart$token")
        )
    }

}