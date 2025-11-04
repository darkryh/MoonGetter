package com.ead.lib.moongetter.examples

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.core.system.extensions.extractWithFallback
import com.ead.lib.moongetter.core.system.extensions.hasVideoExtension
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy

/**
 * Enhanced YourUpload server implementation using new scraping utilities.
 * 
 * This example demonstrates:
 * - Retry mechanism with ExtractionStrategy
 * - Multiple pattern fallback support
 * - String extension functions for cleaner code
 */
class YourUploadEnhanced(
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
            // Primary pattern - most common
            """file:\s*'([^']*)'""",
            // Fallback pattern 1 - quoted with double quotes
            """file:\s*"([^"]*)"""",
            // Fallback pattern 2 - without quotes
            """file:\s*([^\s,]+)""",
            // Fallback pattern 3 - from source tag
            """<source[^>]*src=["']([^"']+)["']"""
        ) ?: throw InvalidServerException(
            Resources.expectedResponseNotFound(name), 
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        // Validate the extracted URL before returning
        val validatedUrl = ExtractionStrategy.withValidation(
            validator = { url -> url.startsWith("http") && url.hasVideoExtension() },
            operation = { videoUrl }
        )

        return listOf(
            Video(
                url = validatedUrl,
                headers = hashMapOf(
                    "Referer" to "https://www.yourupload.com/"
                )
            )
        )
    }

    // Alternative implementation using strategy pattern
    private suspend fun extractWithStrategies(html: String): String {
        return ExtractionStrategy.tryStrategies(
            listOf(
                { extractFromFileProperty(html) },
                { extractFromSourceTag(html) },
                { extractFromVideoTag(html) }
            )
        )
    }

    private fun extractFromFileProperty(html: String): String {
        return html.extractFirst("""file:\s*['"]([^'"]+)['"]""")
            ?: throw InvalidServerException(
                "File property not found",
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )
    }

    private fun extractFromSourceTag(html: String): String {
        return html.extractFirst("""<source[^>]*src=["']([^"']+)["']""")
            ?: throw InvalidServerException(
                "Source tag not found",
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )
    }

    private fun extractFromVideoTag(html: String): String {
        return html.extractFirst("""<video[^>]*src=["']([^"']+)["']""")
            ?: throw InvalidServerException(
                "Video tag not found",
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )
    }
}
