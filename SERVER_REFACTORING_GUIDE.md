# Server Refactoring and Testing Guide

This guide provides templates and best practices for refactoring server implementations to use the new scraping utilities and creating comprehensive tests.

## Refactoring Checklist

When refactoring a server implementation:

- [ ] Add retry mechanism with `ExtractionStrategy.withRetry()`
- [ ] Use pattern fallback with `extractWithFallback()` or `tryMultiplePatterns()`
- [ ] Replace `PatternManager.singleMatch()` with `extractFirst()` extension
- [ ] Use `CommonPatterns` for standard video/iframe/JavaScript patterns
- [ ] Add validation with `withValidation()` when appropriate
- [ ] Ensure proper error handling with specific exceptions
- [ ] Update imports to include new utilities

## Refactoring Template

### Before:
```kotlin
override suspend fun onExtract(): List<Video> {
    val response = client.GET()
    
    if (!response.isSuccess) throw InvalidServerException(...)
    
    return listOf(
        Video(
            url = PatternManager.singleMatch(
                string = response.body.asString().ifEmpty { throw ... },
                regex = """pattern"""
            ) ?: throw InvalidServerException(...)
        )
    )
}
```

### After:
```kotlin
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
        // Primary pattern
        """primary pattern""",
        // Fallback patterns
        """fallback pattern 1""",
        """fallback pattern 2""",
        // Use common patterns when applicable
        CommonPatterns.Video.SOURCE_TAG
    ) ?: throw InvalidServerException(
        Resources.expectedResponseNotFound(name), 
        Error.EXPECTED_RESPONSE_NOT_FOUND
    )

    return listOf(Video(url = videoUrl))
}
```

## Required Imports

```kotlin
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.core.system.extensions.extractWithFallback
import com.ead.lib.moongetter.core.system.extensions.extractAll
import com.ead.lib.moongetter.utils.ExtractionStrategy
import com.ead.lib.moongetter.utils.CommonPatterns
```

## Test Template

### 1. Create Test Directory
```bash
mkdir -p server-src/moongetter-[server]/src/commonTest/kotlin/com/ead/lib/moongetter/[server]
```

### 2. Update build.gradle.kts
Add to `commonTest` dependencies:
```kotlin
commonTest {
    dependencies {
        implementation(libs.kotlin.test)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinxCoroutinesCore.get()}")
        implementation(project(":moongetter-core"))
    }
}
```

### 3. Test File Template

```kotlin
package com.ead.lib.moongetter.[server]

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.ResponseBody
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test suite for [ServerName] server implementation.
 * Tests extraction patterns and error handling with mocked HTTP responses.
 */
class [ServerName]Test {

    private fun createMockClient(htmlResponse: String, statusCode: Int = 200): MoonClient {
        return object : MoonClient {
            override suspend fun <T : Any> request(request: Request): Response {
                return Response(
                    statusCode = statusCode,
                    headers = emptyMap(),
                    body = ResponseBody.StringBody(htmlResponse),
                    url = com.ead.lib.moongetter.client.url.HttpUrl("https://test.example.com/embed/test")
                )
            }

            override suspend fun close() {}
        }
    }

    @Test
    fun testExtract_primaryPattern() = runTest {
        val html = """
            <!-- Sample HTML with video URL -->
        """.trimIndent()

        val client = createMockClient(html)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertEquals("expected-url", videos[0].url)
    }

    @Test
    fun testExtract_fallbackPattern() = runTest {
        val html = """
            <!-- Sample HTML with alternative pattern -->
        """.trimIndent()

        val client = createMockClient(html)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.isNotEmpty())
    }

    @Test
    fun testExtract_unsuccessfulResponse() = runTest {
        val client = createMockClient("", statusCode = 404)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_emptyResponse() = runTest {
        val client = createMockClient("", statusCode = 200)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_noVideoFound() = runTest {
        val html = """
            <html><body><div>No video content</div></body></html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        assertFailsWith<InvalidServerException> {
            server.onExtract()
        }
    }

    @Test
    fun testExtract_withQueryParams() = runTest {
        val html = """
            <!-- HTML with URL containing query parameters -->
        """.trimIndent()

        val client = createMockClient(html)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.contains("?"))
    }

    @Test
    fun testExtract_complexHtml() = runTest {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>Video Player</title></head>
            <body>
                <!-- Complex HTML structure -->
            </body>
            </html>
        """.trimIndent()

        val client = createMockClient(html)
        val server = [ServerName](
            url = "https://example.com/embed/test",
            client = client,
            headers = hashMapOf(),
            configData = Configuration.Data()
        )

        val videos = server.onExtract()

        assertEquals(1, videos.size)
        assertTrue(videos[0].url.isNotEmpty())
    }
}
```

## Test Categories

Every server test should include:

### 1. Success Cases
- ✅ Primary pattern extraction
- ✅ Fallback pattern extraction
- ✅ With query parameters
- ✅ Complex HTML structures

### 2. Error Cases
- ✅ Unsuccessful HTTP response (404, 500, etc.)
- ✅ Empty response body
- ✅ No video found in HTML
- ✅ Malformed HTML

### 3. Edge Cases
- ✅ Multiple video sources
- ✅ Different quality options
- ✅ Various URL formats (absolute, relative)
- ✅ Headers validation (if applicable)

## Common Patterns to Use

### Video Extraction
```kotlin
// Use CommonPatterns for standard cases
html.extractWithFallback(
    """custom pattern""",
    CommonPatterns.Video.MP4_URL,
    CommonPatterns.Video.M3U8_URL,
    CommonPatterns.Video.SOURCE_TAG
)
```

### IFrame Extraction
```kotlin
html.extractUrl(
    CommonPatterns.IFrame.SRC,
    baseUrl = url
)
```

### JavaScript Variable Extraction
```kotlin
html.extractFirst(
    CommonPatterns.JavaScript.variableAssignment("videoUrl")
)
```

## Validation Examples

### URL Validation
```kotlin
ExtractionStrategy.withValidation(
    validator = { it.startsWith("http") && it.hasVideoExtension() }
) {
    extractUrl()
}
```

### Multiple Conditions
```kotlin
ExtractionStrategy.withValidation(
    validator = { url ->
        url.isNotEmpty() && 
        url.startsWith("https://") && 
        url.contains(".mp4")
    }
) {
    extractUrl()
}
```

## Best Practices

1. **Always use retry** - Add `ExtractionStrategy.withRetry()` for network resilience
2. **Provide fallbacks** - Use `extractWithFallback()` with multiple patterns
3. **Use CommonPatterns** - Leverage pre-defined patterns when possible
4. **Validate results** - Use `withValidation()` to ensure extracted URLs are valid
5. **Test thoroughly** - Cover success, error, and edge cases
6. **Mock responses** - Use realistic HTML samples from actual server responses
7. **Document patterns** - Add comments explaining what each pattern matches

## Refactoring Priority

Prioritize servers in this order:

### High Priority (Simple, Common)
1. ✅ YourUpload
2. ✅ Streamtape  
3. ✅ Mp4Upload
4. Pixeldrain
5. Senvid
6. Mediafire

### Medium Priority (Moderate Complexity)
7. Uqload
8. Lulustream
9. Hexload
10. Vihide
11. Mixdrop
12. Doodstream

### Lower Priority (Complex/Special Cases)
13. Vidguard (uses custom decoding)
14. Filemoon (uses JSUnpacker)
15. GoodStream
16. Okru (complex extraction)
17. VK
18. XTwitter
19. Facebook
20. GoogleDrive (API-based)

## Testing Strategy

1. **Create real HTML samples** - Save actual responses from each server
2. **Mock the client** - Use the template above to create mock clients
3. **Test pattern evolution** - Servers change; tests catch when patterns break
4. **Validate error handling** - Ensure appropriate exceptions are thrown
5. **Run tests regularly** - Integrate with CI/CD pipeline

## Additional Resources

- See `examples/` directory for complete implementation examples
- Check `SCRAPING_IMPROVEMENTS.md` for detailed API documentation
- Review existing tests in:
  - `kotlin-src/moongetter-core/src/commonTest/` for utility tests
  - `server-src/moongetter-*/src/commonTest/` for server tests
