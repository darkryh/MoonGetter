package com.ead.lib.moongetter.examples

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.core.system.extensions.extractUrl
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.CommonPatterns
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.PatternManager

/**
 * Example server implementation that demonstrates usage of CommonPatterns library.
 * 
 * Shows how to:
 * - Use pre-defined patterns from CommonPatterns
 * - Combine multiple patterns with alternatives
 * - Handle relative URLs
 * - Extract various video formats
 */
class GenericVideoServer(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val html = ExtractionStrategy.withRetry {
            val response = client.GET()
            if (!response.isSuccess) {
                throw InvalidServerException(
                    Resources.unsuccessfulResponse(name), 
                    Error.UNSUCCESSFUL_RESPONSE, 
                    response.statusCode
                )
            }
            response.body.asString()
        }

        // Try extracting video using multiple strategies
        return ExtractionStrategy.tryStrategies(
            listOf(
                { extractDirectVideo(html) },
                { extractFromIframe(html) },
                { extractFromScript(html) }
            )
        )
    }

    /**
     * Extract direct video URLs from HTML
     */
    private fun extractDirectVideo(html: String): List<Video> {
        // Try multiple video formats using CommonPatterns
        val videoPattern = CommonPatterns.alternatives(
            CommonPatterns.Video.MP4_URL,
            CommonPatterns.Video.M3U8_URL
        )

        val urls = PatternManager.findMultipleMatches(html, videoPattern)
        
        if (urls.isEmpty()) {
            // Try extracting from video/source tags
            val sourceUrl = html.extractFirst(CommonPatterns.Video.SOURCE_TAG)
                ?: html.extractFirst(CommonPatterns.Video.VIDEO_TAG)
                ?: throw InvalidServerException(
                    "No direct video found",
                    Error.EXPECTED_RESPONSE_NOT_FOUND
                )
            
            return listOf(Video(url = sourceUrl))
        }

        return urls.map { Video(url = it) }
    }

    /**
     * Extract video from iframe
     */
    private suspend fun extractFromIframe(html: String): List<Video> {
        // Try both standard and lazy-loaded iframe sources
        val iframeUrl = html.extractUrl(CommonPatterns.IFrame.SRC, baseUrl = url)
            ?: html.extractUrl(CommonPatterns.IFrame.DATA_SRC, baseUrl = url)
            ?: throw InvalidServerException(
                "No iframe found",
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        // Recursively fetch the iframe content
        val iframeResponse = client.GET(requestUrl = iframeUrl)
        if (!iframeResponse.isSuccess) {
            throw InvalidServerException(
                "Failed to fetch iframe",
                Error.UNSUCCESSFUL_RESPONSE
            )
        }

        val iframeHtml = iframeResponse.body.asString()
        
        // Extract video from iframe content
        return extractDirectVideo(iframeHtml)
    }

    /**
     * Extract video URL from JavaScript code
     */
    private fun extractFromScript(html: String): List<Video> {
        // Try extracting from common JavaScript variable patterns
        val videoUrl = html.extractFirst(
            CommonPatterns.JavaScript.variableAssignment("videoUrl")
        ) ?: html.extractFirst(
            CommonPatterns.JavaScript.variableAssignment("file")
        ) ?: html.extractFirst(
            CommonPatterns.JavaScript.variableAssignment("source")
        ) ?: throw InvalidServerException(
            "No video in JavaScript",
            Error.EXPECTED_RESPONSE_NOT_FOUND
        )

        return listOf(Video(url = videoUrl))
    }
}

/**
 * Example showing advanced CommonPatterns usage
 */
class AdvancedPatternsExample(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val html = client.GET().body.asString()

        // Extract domain for constructing absolute URLs
        val domain = PatternManager.singleMatch(html, CommonPatterns.Url.DOMAIN)

        // Extract authentication token if present
        val token = html.extractFirst(CommonPatterns.Auth.TOKEN)
        val apiKey = html.extractFirst(CommonPatterns.Auth.API_KEY)

        // Build pattern to extract content between specific markers
        val betweenPattern = CommonPatterns.between("<video-data>", "</video-data>")
        val videoData = html.extractFirst(betweenPattern)

        // Use optional pattern for parts that may not exist
        val optionalQualityPattern = 
            """src="([^"]+)"${CommonPatterns.optional("""\s+quality="([^"]+)"""")}"""

        // Extract all groups (URL and optional quality)
        val groups = PatternManager.extractAllGroups(html, optionalQualityPattern)
        
        // Extract query parameter
        val videoId = PatternManager.singleMatch(
            url,
            CommonPatterns.Url.queryParam("v")
        )

        // Build video URL with authentication if available
        var videoUrl = html.extractFirst(CommonPatterns.Video.MP4_URL)
            ?: throw InvalidServerException(
                "No video found",
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        // Add authentication if extracted
        if (token != null && !videoUrl.contains("token=")) {
            videoUrl = "$videoUrl${if (videoUrl.contains("?")) "&" else "?"}token=$token"
        }

        return listOf(
            Video(
                url = videoUrl,
                quality = groups.getOrNull(1) ?: DEFAULT
            )
        )
    }
}

/**
 * Demonstrates pattern combination techniques
 */
fun demonstratePatternCombination() {
    println("=== Pattern Combination Examples ===\n")

    // 1. Alternatives - Match any of several patterns
    val anyVideoUrl = CommonPatterns.alternatives(
        CommonPatterns.Video.MP4_URL,
        CommonPatterns.Video.M3U8_URL,
        """https?://[^\s]+\.webm"""
    )
    println("1. Any video URL pattern:")
    println("   $anyVideoUrl\n")

    // 2. Optional parts - Make part of pattern optional
    val urlWithOptionalQuality = 
        """url="([^"]+)"${CommonPatterns.optional("""\s+quality="([^"]+)"""")}"""
    println("2. URL with optional quality:")
    println("   $urlWithOptionalQuality\n")

    // 3. Between markers - Extract content between two strings
    val jsonData = CommonPatterns.between(
        "var config = ",
        ";",
        content = """\{[^\}]+\}"""
    )
    println("3. Extract JSON between markers:")
    println("   $jsonData\n")

    // 4. Complex combination - Real world scenario
    val complexPattern = CommonPatterns.alternatives(
        // Try MP4 in source tag
        CommonPatterns.Video.SOURCE_TAG,
        // Try direct URL
        CommonPatterns.Video.MP4_URL,
        // Try in JavaScript variable
        CommonPatterns.JavaScript.variableAssignment("videoUrl")
    )
    println("4. Complex multi-strategy pattern:")
    println("   $complexPattern\n")
}

fun main() {
    demonstratePatternCombination()
}
