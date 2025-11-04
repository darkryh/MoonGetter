# MoonGetter Scraping Examples

This directory contains practical examples demonstrating the enhanced scraping utilities added to MoonGetter.

## Examples Overview

### 1. YourUploadEnhanced.kt
Demonstrates how to enhance an existing server implementation with:
- **Retry mechanism** - Automatic retry with exponential backoff for network errors
- **Multiple pattern fallback** - Try several regex patterns until one succeeds
- **Validation** - Ensure extracted URLs meet requirements before returning
- **Strategy pattern** - Multiple extraction strategies (file property, source tag, video tag)

**Key features shown:**
```kotlin
// Retry with exponential backoff
val html = ExtractionStrategy.withRetry(RetryConfig.Default) {
    client.GET().body.asString()
}

// Try multiple patterns with fallback
val url = html.extractWithFallback(pattern1, pattern2, pattern3)

// Validate extracted URL
val validUrl = ExtractionStrategy.withValidation(
    validator = { it.startsWith("http") && it.hasVideoExtension() },
    operation = { extractUrl() }
)
```

### 2. DebuggingExample.kt
Shows how to use `ScrapingDebugger` for troubleshooting extraction issues during development:
- **Content analysis** - Get statistics about HTML structure
- **Pattern testing** - Test regex patterns and see results
- **Match contexts** - View text around matches for debugging
- **Sanitization** - Remove sensitive data before logging
- **Offline testing** - Test patterns without making network requests

**Key features shown:**
```kotlin
// Analyze HTML structure
val analysis = ScrapingDebugger.analyzeContent(html)
println(analysis) // Shows: tags present, video references, etc.

// Test a pattern
val test = ScrapingDebugger.testPattern(html, pattern)
println(test) // Shows: match count, extracted values

// View context around matches
val contexts = ScrapingDebugger.getMatchContexts(html, pattern)
contexts.forEach { println(it) }

// Sanitize for logs (removes tokens/keys)
val safe = ScrapingDebugger.sanitizeForLog(response)
```

### 3. CommonPatternsExample.kt
Demonstrates usage of the `CommonPatterns` library with pre-defined regex patterns:
- **Pre-defined patterns** - Use ready-made patterns for common scenarios
- **Pattern combination** - Combine patterns with alternatives, optional, between
- **Multi-format support** - Extract MP4, M3U8, and other video formats
- **iframe extraction** - Handle embedded content
- **JavaScript extraction** - Extract URLs from script tags
- **Authentication** - Extract tokens and API keys

**Key features shown:**
```kotlin
// Use pre-defined patterns
val mp4Urls = html.extractAll(CommonPatterns.Video.MP4_URL)
val m3u8Urls = html.extractAll(CommonPatterns.Video.M3U8_URL)

// Combine patterns with alternatives
val anyVideo = CommonPatterns.alternatives(
    CommonPatterns.Video.MP4_URL,
    CommonPatterns.Video.M3U8_URL
)

// Extract from iframe
val iframeUrl = html.extractUrl(CommonPatterns.IFrame.SRC, baseUrl = url)

// Extract from JavaScript
val videoUrl = html.extractFirst(
    CommonPatterns.JavaScript.variableAssignment("videoUrl")
)

// Extract authentication
val token = html.extractFirst(CommonPatterns.Auth.TOKEN)

// Pattern helpers
val betweenPattern = CommonPatterns.between("<start>", "</end>")
val optionalPattern = CommonPatterns.optional("""\s+quality="([^"]+)"""")
```

## How to Use These Examples

### During Development

1. **Start with debugging** - Use `DebuggingExample.kt` to understand the HTML structure:
   ```kotlin
   // Copy HTML response from browser/network inspector
   val html = """..."""
   val analysis = ScrapingDebugger.analyzeContent(html)
   println(analysis)
   ```

2. **Test patterns offline** - Before implementing in a server:
   ```kotlin
   val testResult = ScrapingDebugger.testPattern(html, myPattern)
   if (testResult.matchCount > 0) {
       // Pattern works, use it in server implementation
   }
   ```

3. **Choose the right pattern** - Check `CommonPatterns` for pre-defined patterns:
   ```kotlin
   // Instead of writing your own regex:
   // val pattern = """https?://[^\s"'<>]+\.mp4(?:\?[^\s"'<>]*)?"""
   
   // Use the pre-defined one:
   val pattern = CommonPatterns.Video.MP4_URL
   ```

4. **Implement with utilities** - Use the enhanced utilities in your server:
   ```kotlin
   class MyServer(...) : Server(...) {
       override suspend fun onExtract(): List<Video> {
           // Use retry for resilience
           val html = ExtractionStrategy.withRetry { client.GET().body.asString() }
           
           // Use fallback patterns
           val url = html.extractWithFallback(pattern1, pattern2, pattern3)
           
           return listOf(Video(url = url))
       }
   }
   ```

### For Testing

Run the debugging example to test patterns offline:

```bash
# If you have a main function in DebuggingExample.kt
kotlinc -script examples/DebuggingExample.kt
```

Or integrate into your test suite:

```kotlin
@Test
fun testPatternExtraction() {
    val html = loadTestHtml()
    val result = ScrapingDebugger.testPattern(html, myPattern)
    assertTrue(result.isValid)
    assertEquals(1, result.matchCount)
}
```

## Best Practices

1. **Always use retry for network operations:**
   ```kotlin
   val html = ExtractionStrategy.withRetry { client.GET().body.asString() }
   ```

2. **Provide fallback patterns for robustness:**
   ```kotlin
   val url = html.extractWithFallback(primaryPattern, fallbackPattern1, fallbackPattern2)
   ```

3. **Use pre-defined patterns when available:**
   ```kotlin
   // Good
   val url = html.extractFirst(CommonPatterns.Video.MP4_URL)
   
   // Less maintainable
   val url = html.extractFirst("""https?://[^\s]+\.mp4""")
   ```

4. **Debug with ScrapingDebugger during development:**
   ```kotlin
   if (debugMode) {
       println(ScrapingDebugger.analyzeContent(html))
       println(ScrapingDebugger.testPattern(html, pattern))
   }
   ```

5. **Validate extracted data:**
   ```kotlin
   val validUrl = ExtractionStrategy.withValidation(
       validator = { it.startsWith("https://") },
       operation = { extractUrl() }
   )
   ```

6. **Use strategy pattern for multiple approaches:**
   ```kotlin
   val url = ExtractionStrategy.tryStrategies(listOf(
       { extractMethod1(html) },
       { extractMethod2(html) },
       { extractMethod3(html) }
   ))
   ```

## Common Patterns Reference

### Video Patterns
- `CommonPatterns.Video.MP4_URL` - MP4 video URLs
- `CommonPatterns.Video.M3U8_URL` - HLS playlist URLs
- `CommonPatterns.Video.SOURCE_TAG` - `<source>` tag src
- `CommonPatterns.Video.VIDEO_TAG` - `<video>` tag src

### IFrame Patterns
- `CommonPatterns.IFrame.SRC` - Standard iframe src
- `CommonPatterns.IFrame.DATA_SRC` - Lazy-loaded data-src

### JavaScript Patterns
- `CommonPatterns.JavaScript.PACKED` - Packed/obfuscated JS
- `CommonPatterns.JavaScript.variableAssignment(name)` - Variable = value
- `CommonPatterns.JavaScript.jsonAssignment(name)` - JSON object assignment

### URL Patterns
- `CommonPatterns.Url.DOMAIN` - Extract domain
- `CommonPatterns.Url.queryParam(name)` - Extract query parameter
- `CommonPatterns.Url.ANY_URL` - Match any URL

### Auth Patterns
- `CommonPatterns.Auth.TOKEN` - Authentication tokens
- `CommonPatterns.Auth.API_KEY` - API keys

### Pattern Helpers
- `CommonPatterns.alternatives(p1, p2, ...)` - Match any pattern
- `CommonPatterns.optional(pattern)` - Make pattern optional
- `CommonPatterns.between(start, end)` - Content between markers

## Migration from Old Style

### Before:
```kotlin
override suspend fun onExtract(): List<Video> {
    val response = client.GET()
    val body = response.body.asString()
    val regex = Regex("""file:\s*'([^']*)'""")
    val match = regex.find(body)
    val url = match?.groups?.get(1)?.value
        ?: throw InvalidServerException("Not found", Error.EXPECTED_RESPONSE_NOT_FOUND)
    return listOf(Video(url = url))
}
```

### After:
```kotlin
override suspend fun onExtract(): List<Video> {
    val html = ExtractionStrategy.withRetry {
        client.GET().body.asString()
    }
    
    val url = html.extractWithFallback(
        """file:\s*'([^']*)'""",
        """file:\s*"([^"]*)"""",
        CommonPatterns.Video.SOURCE_TAG
    ) ?: throw InvalidServerException("Not found", Error.EXPECTED_RESPONSE_NOT_FOUND)
    
    return listOf(Video(url = url))
}
```

## Need Help?

- See `SCRAPING_IMPROVEMENTS.md` in the root directory for detailed documentation
- Check the source code of enhanced utilities in `kotlin-src/moongetter-core/src/commonMain/kotlin/com/ead/lib/moongetter/utils/`
- Look at existing server implementations in `server-src/` for real-world usage
