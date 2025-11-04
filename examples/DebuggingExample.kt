package com.ead.lib.moongetter.examples

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.CommonPatterns
import com.ead.lib.moongetter.utils.ScrapingDebugger

/**
 * Example demonstrating how to use ScrapingDebugger for troubleshooting
 * extraction issues.
 * 
 * This is useful during development when patterns don't work as expected.
 */
class ServerWithDebugging(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    // Enable debug mode via config (you can add this to Configuration.Data)
    private val debugMode = configData.timeout > 10000L

    override suspend fun onExtract(): List<Video> {
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

        // Debug: Analyze the content structure
        if (debugMode) {
            val analysis = ScrapingDebugger.analyzeContent(html)
            println("=== Content Analysis ===")
            println(analysis)
        }

        // Pattern to try
        val pattern = CommonPatterns.Video.MP4_URL

        // Debug: Test the pattern before using it
        if (debugMode) {
            val testResult = ScrapingDebugger.testPattern(html, pattern)
            println("\n=== Pattern Test ===")
            println(testResult)

            // Show context around matches
            if (testResult.matchCount > 0) {
                println("\n=== Match Contexts ===")
                val contexts = ScrapingDebugger.getMatchContexts(html, pattern)
                contexts.forEach { context ->
                    println(context)
                }
            }
        }

        // Extract the video URL
        val videoUrl = html.extractFirst(pattern)
            ?: throw InvalidServerException(
                Resources.expectedResponseNotFound(name), 
                Error.EXPECTED_RESPONSE_NOT_FOUND
            )

        // Debug: Log sanitized result
        if (debugMode) {
            println("\n=== Extracted URL (sanitized) ===")
            println(ScrapingDebugger.sanitizeForLog(videoUrl))
        }

        return listOf(Video(url = videoUrl))
    }
}

/**
 * Example showing how to test patterns offline before implementing them in a server.
 */
fun debugPatternOffline() {
    // Sample HTML from a server response (you would get this from actual response)
    val sampleHtml = """
        <html>
        <head><title>Video</title></head>
        <body>
            <video controls>
                <source src="https://example.com/video.mp4?token=abc123" type="video/mp4">
            </video>
            <script>
                var videoUrl = "https://example.com/hls/playlist.m3u8";
                var apiKey = "secret_key_12345";
            </script>
        </body>
        </html>
    """.trimIndent()

    println("=== Analyzing Sample HTML ===")
    
    // 1. Analyze content
    val analysis = ScrapingDebugger.analyzeContent(sampleHtml)
    println(analysis)
    
    // 2. Test MP4 pattern
    println("\n=== Testing MP4 Pattern ===")
    val mp4Test = ScrapingDebugger.testPattern(
        sampleHtml, 
        CommonPatterns.Video.MP4_URL
    )
    println(mp4Test)
    
    // 3. Test M3U8 pattern
    println("\n=== Testing M3U8 Pattern ===")
    val m3u8Test = ScrapingDebugger.testPattern(
        sampleHtml, 
        CommonPatterns.Video.M3U8_URL
    )
    println(m3u8Test)
    
    // 4. Test custom pattern for API key
    println("\n=== Testing API Key Pattern ===")
    val apiKeyTest = ScrapingDebugger.testPattern(
        sampleHtml,
        """apiKey\s*=\s*"([^"]+)""""
    )
    println(apiKeyTest)
    
    // 5. Show match contexts
    println("\n=== Match Contexts for M3U8 ===")
    val contexts = ScrapingDebugger.getMatchContexts(
        sampleHtml,
        CommonPatterns.Video.M3U8_URL,
        contextLength = 30
    )
    contexts.forEach { println(it) }
    
    // 6. Sanitize for logging
    println("\n=== Sanitized HTML ===")
    println(ScrapingDebugger.sanitizeForLog(sampleHtml, maxLength = 200))
}

// Run this to test patterns offline
fun main() {
    debugPatternOffline()
}
