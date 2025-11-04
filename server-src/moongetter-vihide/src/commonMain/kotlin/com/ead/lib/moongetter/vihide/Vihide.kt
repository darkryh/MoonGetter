package com.ead.lib.moongetter.vihide

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.js.unpacker.JsUnpacker
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.CommonPatterns
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.PlaylistUtils

@ExperimentalServer
class Vihide(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    override val headers: HashMap<String, String> = headers.also {
        it["Origin"] = url
        it["Referer"] = url
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

        // Detect and unpack if needed
        val processedHtml = if (JsUnpacker.detect(html)) {
            JsUnpacker.unpackAndCombine(html) 
                ?: throw InvalidServerException(
                    Resources.expectedPackedResponseNotFound(name), 
                    Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND
                )
        } else {
            html
        }

        // Extract M3U8 URL with custom pattern or CommonPatterns
        val playlistUrl = processedHtml.extractFirst(
            """"[^"]*"\s*:\s*"(https?://[^"]+\.m3u8[^"]*)""""
        ) ?: processedHtml.extractFirst(CommonPatterns.Video.M3U8_URL)
            ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        return playlistUtils.extractFromHls(playlistUrl)
    }
}