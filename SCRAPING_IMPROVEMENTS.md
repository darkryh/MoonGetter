# Scraping Module Improvements

This document describes the enhancements made to the MoonGetter scraping modules to support specific changes and improve maintainability.

## Overview

The scraping module improvements focus on making it easier to customize extraction logic, debug issues, and maintain server implementations across the multi-module library.

## New Features

### 1. Enhanced Pattern Manager (`PatternManager.kt`)

**New Methods:**

- `tryMultiplePatterns()` - Try multiple regex patterns sequentially until one matches
  - Useful when different servers use different HTML structures
  - Example: `PatternManager.tryMultiplePatterns(html, listOf(pattern1, pattern2, pattern3))`

- `extractAllGroups()` - Extract all capture groups from the first match
  - Returns all captured groups as a list
  - Example: `PatternManager.extractAllGroups(html, """src="([^"]+)" quality="([^"]+)""")`

- `findAllMatchesWithAllGroups()` - Find all matches and return all groups for each
  - Useful for extracting multiple videos with multiple attributes
  - Example: `PatternManager.findAllMatchesWithAllGroups(html, videoPattern)`

- `isValidPattern()` - Validate regex pattern syntax
  - Helps catch pattern errors early
  - Example: `PatternManager.isValidPattern(userPattern)`

- `extractAndTransform()` - Extract and transform in one step
  - Apply custom transformation to extracted value
  - Example: `PatternManager.extractAndTransform(html, pattern) { it.toUpperCase() }`

- `extractUrl()` - Smart URL extraction with relative URL resolution
  - Handles absolute, relative, and protocol-relative URLs
  - Example: `PatternManager.extractUrl(html, pattern, baseUrl = "https://example.com")`

### 2. Extraction Strategy Utilities (`ExtractionStrategy.kt`)

**Retry Mechanisms:**

- `RetryConfig` - Configure retry behavior
  - `maxAttempts` - Number of retry attempts
  - `delayMillis` - Delay between retries
  - `backoffMultiplier` - Exponential backoff multiplier
  - Presets: `Default`, `NoRetry`, `Aggressive`

- `withRetry()` - Execute extraction with automatic retry
  ```kotlin
  val video = ExtractionStrategy.withRetry(RetryConfig.Default) {
      extractVideoUrl()
  }
  ```

**Strategy Patterns:**

- `tryStrategies()` - Try multiple extraction strategies
  ```kotlin
  val result = ExtractionStrategy.tryStrategies(listOf(
      { extractFromIframe() },
      { extractFromSource() },
      { extractFromScript() }
  ))
  ```

- `withFallback()` - Primary strategy with fallback
  ```kotlin
  val url = ExtractionStrategy.withFallback(
      primary = { extractHDQuality() },
      fallback = { extractSDQuality() }
  )
  ```

- `withValidation()` - Retry until validation passes
  ```kotlin
  val url = ExtractionStrategy.withValidation(
      validator = { it.startsWith("https://") },
      operation = { extractUrl() }
  )
  ```

### 3. Scraping Debugger (`ScrapingDebugger.kt`)

**Content Analysis:**

- `analyzeContent()` - Get statistics about HTML content
  ```kotlin
  val analysis = ScrapingDebugger.analyzeContent(html)
  println(analysis) // Shows: length, line count, tag presence, etc.
  ```

**Pattern Testing:**

- `testPattern()` - Test regex pattern against content
  ```kotlin
  val result = ScrapingDebugger.testPattern(html, pattern)
  println(result) // Shows: validity, match count, extracted values
  ```

- `getMatchContexts()` - Show context around matches
  ```kotlin
  val contexts = ScrapingDebugger.getMatchContexts(html, pattern)
  contexts.forEach { println(it) } // Shows text before/after each match
  ```

**Logging:**

- `sanitizeForLog()` - Remove sensitive data from logs
  ```kotlin
  val safe = ScrapingDebugger.sanitizeForLog(response)
  logger.debug(safe) // Tokens and keys are masked
  ```

### 4. Common Patterns Library (`CommonPatterns.kt`)

Pre-defined regex patterns for common extraction tasks:

**Video Patterns:**
- `CommonPatterns.Video.MP4_URL` - Match MP4 video URLs
- `CommonPatterns.Video.M3U8_URL` - Match M3U8 playlist URLs
- `CommonPatterns.Video.SOURCE_TAG` - Extract from `<source>` tags
- `CommonPatterns.Video.VIDEO_TAG` - Extract from `<video>` tags

**IFrame Patterns:**
- `CommonPatterns.IFrame.SRC` - Standard iframe src
- `CommonPatterns.IFrame.DATA_SRC` - Lazy-loaded iframe data-src

**JavaScript Patterns:**
- `CommonPatterns.JavaScript.PACKED` - Packed JavaScript code
- `CommonPatterns.JavaScript.variableAssignment()` - Variable assignments
- `CommonPatterns.JavaScript.jsonAssignment()` - JSON object assignments

**URL Patterns:**
- `CommonPatterns.Url.DOMAIN` - Extract domain from URL
- `CommonPatterns.Url.queryParam()` - Extract query parameter
- `CommonPatterns.Url.ANY_URL` - Match any URL

**Auth Patterns:**
- `CommonPatterns.Auth.TOKEN` - Extract authentication tokens
- `CommonPatterns.Auth.API_KEY` - Extract API keys

**Pattern Helpers:**
- `alternatives()` - Combine multiple patterns with OR
- `optional()` - Make a pattern optional
- `between()` - Match content between two strings

### 5. String Extensions (`String.kt`)

Convenient extension functions for scraping:

```kotlin
// Extract first match
val url = html.extractFirst("""src="([^"]+)"""")

// Extract all matches
val urls = html.extractAll("""href="([^"]+)"""")

// Try multiple patterns with fallback
val token = html.extractWithFallback(
    """token:["']([^"']+)["']""",
    """token=([^&]+)""",
    """"token":"([^"]+)""""
)

// Check if matches any pattern
if (html.matchesAny("""\.mp4""", """\.m3u8""")) {
    // Contains video reference
}

// Extract URL with resolution
val videoUrl = html.extractUrl("""src="([^"]+)"""", baseUrl = "https://example.com")

// Extract all groups
val (quality, url) = html.extractGroups("""quality="([^"]+)" src="([^"]+)"""")

// Clean HTML
val text = html.stripHtmlTags()

// Decode entities
val decoded = html.decodeHtmlEntities()

// Extract JSON value
val apiKey = json.extractJsonValue("api_key")

// Check for video extensions
if (url.hasVideoExtension()) {
    // Is a video URL
}
```

## Usage Examples

### Example 1: Enhanced Server Implementation

```kotlin
class MyServer(
    url: String,
    client: MoonClient,
    headers: HashMap<String, String>,
    configData: Configuration.Data
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        // Use retry mechanism
        val html = ExtractionStrategy.withRetry(RetryConfig.Default) {
            client.GET().body.asString()
        }

        // Try multiple extraction strategies
        val videoUrl = ExtractionStrategy.tryStrategies(listOf(
            { extractFromIframe(html) },
            { extractFromSource(html) },
            { extractFromScript(html) }
        ))

        return listOf(Video(url = videoUrl))
    }

    private fun extractFromIframe(html: String): String {
        return html.extractUrl(CommonPatterns.IFrame.SRC, baseUrl = url)
            ?: throw InvalidServerException("No iframe found", Error.EXPECTED_RESPONSE_NOT_FOUND)
    }

    private fun extractFromSource(html: String): String {
        return html.extractFirst(CommonPatterns.Video.SOURCE_TAG)
            ?: throw InvalidServerException("No source found", Error.EXPECTED_RESPONSE_NOT_FOUND)
    }

    private fun extractFromScript(html: String): String {
        // Try multiple patterns
        return html.extractWithFallback(
            """videoUrl\s*=\s*["']([^"']+)["']""",
            """src:\s*["']([^"']+)["']""",
            """"file":\s*"([^"]+)""""
        ) ?: throw InvalidServerException("No video in script", Error.EXPECTED_RESPONSE_NOT_FOUND)
    }
}
```

### Example 2: Debugging Extraction Issues

```kotlin
// Analyze content structure
val analysis = ScrapingDebugger.analyzeContent(html)
println(analysis) // See what tags and patterns are present

// Test your patterns
val test = ScrapingDebugger.testPattern(html, myPattern)
println(test) // See matches and extracted values

// View context around matches
val contexts = ScrapingDebugger.getMatchContexts(html, myPattern, contextLength = 100)
contexts.forEach { println(it) }
```

### Example 3: Using Common Patterns

```kotlin
override suspend fun onExtract(): List<Video> {
    val html = client.GET().body.asString()

    // Use pre-defined patterns
    val mp4Urls = html.extractAll(CommonPatterns.Video.MP4_URL)
    val m3u8Urls = html.extractAll(CommonPatterns.Video.M3U8_URL)

    // Combine patterns
    val anyVideoUrl = html.extractFirst(
        CommonPatterns.alternatives(
            CommonPatterns.Video.MP4_URL,
            CommonPatterns.Video.M3U8_URL
        )
    )

    return (mp4Urls + m3u8Urls).map { Video(url = it) }
}
```

## Benefits

1. **Reduced Code Duplication** - Common patterns and utilities shared across servers
2. **Better Error Handling** - Automatic retry and fallback mechanisms
3. **Easier Debugging** - Built-in tools for analyzing and testing patterns
4. **More Maintainable** - Clear separation of concerns and reusable components
5. **Flexible Customization** - Easy to adapt extraction logic for specific server changes
6. **Type-Safe Patterns** - Compile-time validation of pattern usage

## Migration Guide

### Before:
```kotlin
override suspend fun onExtract(): List<Video> {
    val response = client.GET()
    val html = response.body.asString()
    
    val match = Regex("""src="([^"]+)"""").find(html)
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
    
    val url = html.extractFirst(CommonPatterns.Video.SOURCE_TAG)
        ?: throw InvalidServerException("Not found", Error.EXPECTED_RESPONSE_NOT_FOUND)
    
    return listOf(Video(url = url))
}
```

## Best Practices

1. **Use Common Patterns** - Leverage pre-defined patterns when possible
2. **Add Retry Logic** - Use `ExtractionStrategy.withRetry()` for network operations
3. **Provide Fallbacks** - Use `tryStrategies()` or `withFallback()` for robust extraction
4. **Debug First** - Use `ScrapingDebugger` tools when patterns don't work
5. **Validate Results** - Use `withValidation()` to ensure extracted data is correct
6. **Document Patterns** - Comment complex regex patterns explaining what they match

## Testing

When implementing or modifying servers:

1. Use `ScrapingDebugger.analyzeContent()` to understand the HTML structure
2. Test patterns with `ScrapingDebugger.testPattern()` before using them
3. Use `PatternManager.isValidPattern()` to validate regex syntax
4. Add retry logic for flaky servers
5. Provide multiple extraction strategies for robustness
