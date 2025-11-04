package com.ead.lib.moongetter.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ScrapingDebuggerTest {

    @Test
    fun testAnalyzeContent_basicHtml() {
        val html = """
            <html>
            <head><title>Test</title></head>
            <body>
                <video src="test.mp4"></video>
            </body>
            </html>
        """.trimIndent()
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.length > 0)
        assertTrue(analysis.lineCount > 0)
        assertTrue(analysis.hasVideoTags)
        assertTrue(analysis.hasMp4References)
        assertFalse(analysis.hasIframeTags)
        assertFalse(analysis.hasM3u8References)
    }

    @Test
    fun testAnalyzeContent_withScriptTags() {
        val html = """
            <html>
            <script>var videoUrl = "test.mp4";</script>
            </html>
        """.trimIndent()
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.hasScriptTags)
        assertTrue(analysis.hasMp4References)
    }

    @Test
    fun testAnalyzeContent_withIframe() {
        val html = """
            <html>
            <iframe src="https://example.com/video"></iframe>
            </html>
        """.trimIndent()
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.hasIframeTags)
    }

    @Test
    fun testAnalyzeContent_withSourceTag() {
        val html = """
            <video>
                <source src="video.mp4" type="video/mp4">
            </video>
        """.trimIndent()
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.hasSourceTags)
        assertTrue(analysis.hasVideoTags)
        assertTrue(analysis.hasMp4References)
    }

    @Test
    fun testAnalyzeContent_withM3u8() {
        val html = """
            <video src="https://example.com/playlist.m3u8"></video>
        """.trimIndent()
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.hasM3u8References)
        assertTrue(analysis.hasVideoTags)
    }

    @Test
    fun testAnalyzeContent_withPackedJs() {
        val html = """
            <script>
            eval(function(p,a,c,k,e,d){})
            </script>
        """.trimIndent()
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.containsPackedJs)
    }

    @Test
    fun testAnalyzeContent_caseInsensitive() {
        val html = """<VIDEO src="TEST.MP4"></VIDEO>"""
        
        val analysis = ScrapingDebugger.analyzeContent(html)
        
        assertTrue(analysis.hasVideoTags)
        assertTrue(analysis.hasMp4References)
    }

    @Test
    fun testTestPattern_validPattern() {
        val html = """<video src="https://example.com/video.mp4"></video>"""
        val pattern = """src="([^"]+)""""
        
        val result = ScrapingDebugger.testPattern(html, pattern)
        
        assertTrue(result.isValid)
        assertEquals(1, result.matchCount)
        assertEquals(1, result.extractedValues.size)
        assertEquals("https://example.com/video.mp4", result.extractedValues[0])
    }

    @Test
    fun testTestPattern_multipleMatches() {
        val html = """
            <source src="video1.mp4">
            <source src="video2.mp4">
            <source src="video3.mp4">
        """.trimIndent()
        
        val result = ScrapingDebugger.testPattern(html, """src="([^"]+)"""")
        
        assertTrue(result.isValid)
        assertEquals(3, result.matchCount)
        assertEquals(3, result.extractedValues.size)
    }

    @Test
    fun testTestPattern_noMatch() {
        val html = """<div>No video here</div>"""
        
        val result = ScrapingDebugger.testPattern(html, """src="([^"]+)"""")
        
        assertTrue(result.isValid)
        assertEquals(0, result.matchCount)
        assertTrue(result.extractedValues.isEmpty())
    }

    @Test
    fun testTestPattern_invalidPattern() {
        val html = """<video src="test.mp4"></video>"""
        val invalidPattern = """src="([^"]+""" // Missing closing )
        
        val result = ScrapingDebugger.testPattern(html, invalidPattern)
        
        assertFalse(result.isValid)
        assertEquals(0, result.matchCount)
        assertTrue(result.extractedValues.isEmpty())
        assertTrue(result.error != null)
    }

    @Test
    fun testTestPattern_withGroupIndex() {
        val html = """<video quality="720p" src="https://example.com/video.mp4"></video>"""
        
        val result = ScrapingDebugger.testPattern(
            html,
            """quality="([^"]+)"\s+src="([^"]+)"""",
            groupIndex = 2
        )
        
        assertTrue(result.isValid)
        assertEquals(1, result.matchCount)
        assertEquals("https://example.com/video.mp4", result.extractedValues[0])
    }

    @Test
    fun testGetMatchContexts_singleMatch() {
        val html = """before <video src="test.mp4"> after"""
        
        val contexts = ScrapingDebugger.getMatchContexts(html, """src="[^"]+"""", contextLength = 10)
        
        assertEquals(1, contexts.size)
        val context = contexts[0]
        assertTrue(context.beforeContext.contains("before"))
        assertTrue(context.matchedText.contains("src"))
        assertTrue(context.afterContext.contains("after"))
    }

    @Test
    fun testGetMatchContexts_multipleMatches() {
        val html = """
            first <video src="video1.mp4"> middle <video src="video2.mp4"> last
        """.trimIndent()
        
        val contexts = ScrapingDebugger.getMatchContexts(html, """src="[^"]+"""")
        
        assertEquals(2, contexts.size)
    }

    @Test
    fun testGetMatchContexts_limitToFive() {
        val html = (1..10).joinToString("\n") { """<video src="video$it.mp4">""" }
        
        val contexts = ScrapingDebugger.getMatchContexts(html, """src="[^"]+"""")
        
        assertEquals(5, contexts.size) // Limited to 5
    }

    @Test
    fun testSanitizeForLog_removeTokens() {
        val content = "https://example.com/video.mp4?token=secret123&quality=720p"
        
        val sanitized = ScrapingDebugger.sanitizeForLog(content)
        
        assertTrue(sanitized.contains("token=***"))
        assertFalse(sanitized.contains("secret123"))
        assertTrue(sanitized.contains("quality=720p"))
    }

    @Test
    fun testSanitizeForLog_removeApiKey() {
        val content = "api_key=secret_key_123"
        
        val sanitized = ScrapingDebugger.sanitizeForLog(content)
        
        assertTrue(sanitized.contains("api_key=***"))
        assertFalse(sanitized.contains("secret_key_123"))
    }

    @Test
    fun testSanitizeForLog_removeKey() {
        val content = "key=mykey123"
        
        val sanitized = ScrapingDebugger.sanitizeForLog(content)
        
        assertTrue(sanitized.contains("key=***"))
        assertFalse(sanitized.contains("mykey123"))
    }

    @Test
    fun testSanitizeForLog_truncateLongContent() {
        val content = "a".repeat(1000)
        
        val sanitized = ScrapingDebugger.sanitizeForLog(content, maxLength = 100)
        
        assertTrue(sanitized.length <= 120) // 100 + "... (truncated)"
        assertTrue(sanitized.contains("truncated"))
    }

    @Test
    fun testSanitizeForLog_noTruncationIfShort() {
        val content = "short content"
        
        val sanitized = ScrapingDebugger.sanitizeForLog(content, maxLength = 100)
        
        assertEquals(content, sanitized)
        assertFalse(sanitized.contains("truncated"))
    }

    @Test
    fun testContentAnalysisToString() {
        val analysis = ScrapingDebugger.ContentAnalysis(
            length = 1000,
            lineCount = 50,
            hasScriptTags = true,
            hasIframeTags = false,
            hasVideoTags = true,
            hasSourceTags = true,
            hasM3u8References = false,
            hasMp4References = true,
            containsPackedJs = false
        )
        
        val string = analysis.toString()
        
        assertTrue(string.contains("1000 chars"))
        assertTrue(string.contains("50"))
        assertTrue(string.contains("true"))
        assertTrue(string.contains("false"))
    }

    @Test
    fun testPatternTestResultToString() {
        val result = ScrapingDebugger.PatternTestResult(
            isValid = true,
            matchCount = 2,
            extractedValues = listOf("value1", "value2"),
            error = null
        )
        
        val string = result.toString()
        
        assertTrue(string.contains("Valid: true"))
        assertTrue(string.contains("Matches: 2"))
        assertTrue(string.contains("value1"))
        assertTrue(string.contains("value2"))
    }

    @Test
    fun testMatchContextToString() {
        val context = ScrapingDebugger.MatchContext(
            beforeContext = "before text",
            matchedText = "matched",
            afterContext = "after text",
            position = 100
        )
        
        val string = context.toString()
        
        assertTrue(string.contains("before text"))
        assertTrue(string.contains("matched"))
        assertTrue(string.contains("after text"))
        assertTrue(string.contains("100"))
    }
}
