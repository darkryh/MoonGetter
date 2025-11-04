@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.filemoon

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
import com.ead.lib.moongetter.utils.Values.targetUrl

@ExperimentalServer
class Filemoon(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val playlistUtils by lazy { PlaylistUtils(client, headers) }

    override val headers: HashMap<String, String> = headers.also {
        it["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        it["Accept-Language"] = "en-US,en;q=0.5"
        it["Priority"] = "u=0, i"
        it["Origin"] = url
        it["Referer"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        // First request with retry
        val firstResponse = ExtractionStrategy.withRetry(
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
            
            val html = response.body.asString().ifEmpty { 
                throw InvalidServerException(
                    Resources.emptyOrNullResponse(name), 
                    Error.EMPTY_OR_NULL_RESPONSE
                ) 
            }
            
            Pair(html, response.headers)
        }

        // Extract iframe URL using CommonPatterns
        url = firstResponse.first.extractFirst(CommonPatterns.IFrame.SRC)
            ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        // Second request with retry
        val secondHtml = ExtractionStrategy.withRetry(
            config = ExtractionStrategy.RetryConfig.Default
        ) {
            val response = client.GET(
                overrideHeaders = firstResponse.second
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

        // Unpack JavaScript
        val unpacked = JsUnpacker.unpackAndCombine(secondHtml)
            ?: throw InvalidServerException(
                Resources.expectedPackedResponseNotFound(name), 
                Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND
            )

        // Extract M3U8 URL using CommonPatterns
        val playlistUrl = unpacked.extractFirst(CommonPatterns.Video.M3U8_URL)
            ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        return playlistUtils.extractFromHls(playlistUrl)
    }
}