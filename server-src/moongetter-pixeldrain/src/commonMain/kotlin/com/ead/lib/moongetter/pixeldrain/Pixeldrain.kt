package com.ead.lib.moongetter.pixeldrain

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy

class Pixeldrain(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        // Extract ID from URL using pattern extraction
        val id = url.extractFirst("""/u/([a-zA-Z0-9]+)""")
            ?: throw InvalidServerException(
                "Invalid Pixeldrain URL format",
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        // Construct direct download URL
        url = "https://pixeldrain.com/api/file/$id?download"

        // Use retry mechanism for resilient extraction
        ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val response = client.GET(isResponseBodyNeeded = false)
            
            if (!response.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name), 
                    Error.UNSUCCESSFUL_RESPONSE, 
                    response.statusCode
                )
            }
        }

        return listOf(Video(url = url))
    }
}