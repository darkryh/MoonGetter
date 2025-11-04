package com.ead.lib.moongetter.mixdrop

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.js.unpacker.JsUnpacker
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy

@ExperimentalServer
class Mixdrop(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = DEFAULT_REFERER
        it["Origin"] = DEFAULT_REFERER
    }

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

        // Check for packed JavaScript
        if (!JsUnpacker.detect(html)) {
            throw InvalidServerException(
                Resources.expectedPackedResponseNotFound(name), 
                Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND
            )
        }

        // Extract eval block
        val evalBlock = html.extractFirst(
            """eval(.*)""",
            groupIndex = 0
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        // Unpack JavaScript
        val unpacked = JsUnpacker.unpackAndCombine(evalBlock)
            ?: throw InvalidServerException(
                Resources.expectedPackedResponseNotFound(name), 
                Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND
            )

        // Extract URL from unpacked content
        val videoPath = unpacked.extractFirst(
            """wurl="?\"(.*?)\";"""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = "https:$videoPath",
                    headers = headers,
                    method = "GET"
                )
            )
        )
    }

    companion object {
        private const val DEFAULT_REFERER = "https://mixdrop.co/"
    }
}