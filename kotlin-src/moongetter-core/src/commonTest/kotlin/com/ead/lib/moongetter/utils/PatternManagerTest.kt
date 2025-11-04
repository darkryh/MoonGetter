package com.ead.lib.moongetter.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PatternManagerTest {

    @Test
    fun testSingleMatch_basicPattern() {
        val html = """<video src="https://example.com/video.mp4"></video>"""
        val result = PatternManager.singleMatch(html, """src="([^"]+)"""")
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testSingleMatch_noMatch() {
        val html = """<div>No video here</div>"""
        val result = PatternManager.singleMatch(html, """src="([^"]+)"""")
        
        assertNull(result)
    }

    @Test
    fun testSingleMatch_withGroupIndex() {
        val html = """<video quality="720p" src="https://example.com/video.mp4"></video>"""
        val result = PatternManager.singleMatch(
            html, 
            """quality="([^"]+)"\s+src="([^"]+)"""",
            groupIndex = 2
        )
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testFindMultipleMatches_multipleVideos() {
        val html = """
            <source src="https://example.com/video1.mp4">
            <source src="https://example.com/video2.mp4">
            <source src="https://example.com/video3.mp4">
        """.trimIndent()
        
        val results = PatternManager.findMultipleMatches(html, """src="([^"]+)"""")
        
        assertEquals(3, results.size)
        assertEquals("https://example.com/video1.mp4", results[0])
        assertEquals("https://example.com/video2.mp4", results[1])
        assertEquals("https://example.com/video3.mp4", results[2])
    }

    @Test
    fun testFindMultipleMatchesAsPairs() {
        val html = """
            <source quality="720p" url="https://example.com/video1.mp4">
            <source quality="1080p" url="https://example.com/video2.mp4">
        """.trimIndent()
        
        val results = PatternManager.findMultipleMatchesAsPairs(
            html,
            """quality="([^"]+)"\s+url="([^"]+)""""
        )
        
        assertEquals(2, results.size)
        assertEquals("720p" to "https://example.com/video1.mp4", results[0])
        assertEquals("1080p" to "https://example.com/video2.mp4", results[1])
    }

    @Test
    fun testMatch_patternExists() {
        val html = """<video src="test.mp4"></video>"""
        val result = PatternManager.match("""\.mp4""", html)
        
        assertTrue(result)
    }

    @Test
    fun testMatch_patternNotExists() {
        val html = """<video src="test.avi"></video>"""
        val result = PatternManager.match("""\.mp4""", html)
        
        assertFalse(result)
    }

    @Test
    fun testTryMultiplePatterns_firstMatches() {
        val html = """var videoUrl = "https://example.com/video.mp4";"""
        val patterns = listOf(
            """videoUrl\s*=\s*"([^"]+)"""",
            """file:\s*'([^']+)'""",
            """src="([^"]+)""""
        )
        
        val result = PatternManager.tryMultiplePatterns(html, patterns)
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testTryMultiplePatterns_secondMatches() {
        val html = """file: 'https://example.com/video.mp4'"""
        val patterns = listOf(
            """videoUrl\s*=\s*"([^"]+)"""",
            """file:\s*'([^']+)'""",
            """src="([^"]+)""""
        )
        
        val result = PatternManager.tryMultiplePatterns(html, patterns)
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testTryMultiplePatterns_noneMatch() {
        val html = """<div>No video here</div>"""
        val patterns = listOf(
            """videoUrl\s*=\s*"([^"]+)"""",
            """file:\s*'([^']+)'""",
            """src="([^"]+)""""
        )
        
        val result = PatternManager.tryMultiplePatterns(html, patterns)
        
        assertNull(result)
    }

    @Test
    fun testExtractAllGroups() {
        val html = """<video quality="720p" src="https://example.com/video.mp4" type="video/mp4"></video>"""
        val result = PatternManager.extractAllGroups(
            html,
            """quality="([^"]+)"\s+src="([^"]+)"\s+type="([^"]+)""""
        )
        
        assertEquals(3, result.size)
        assertEquals("720p", result[0])
        assertEquals("https://example.com/video.mp4", result[1])
        assertEquals("video/mp4", result[2])
    }

    @Test
    fun testExtractAllGroups_noMatch() {
        val html = """<div>No video</div>"""
        val result = PatternManager.extractAllGroups(html, """src="([^"]+)"""")
        
        assertTrue(result.isEmpty())
    }

    @Test
    fun testFindAllMatchesWithAllGroups() {
        val html = """
            <source quality="720p" src="https://example.com/video1.mp4">
            <source quality="1080p" src="https://example.com/video2.mp4">
        """.trimIndent()
        
        val results = PatternManager.findAllMatchesWithAllGroups(
            html,
            """quality="([^"]+)"\s+src="([^"]+)""""
        )
        
        assertEquals(2, results.size)
        assertEquals(listOf("720p", "https://example.com/video1.mp4"), results[0])
        assertEquals(listOf("1080p", "https://example.com/video2.mp4"), results[1])
    }

    @Test
    fun testIsValidPattern_valid() {
        val result = PatternManager.isValidPattern("""src="([^"]+)"""")
        assertTrue(result)
    }

    @Test
    fun testIsValidPattern_invalid() {
        val result = PatternManager.isValidPattern("""src="([^"]+""") // Missing closing )
        assertFalse(result)
    }

    @Test
    fun testExtractAndTransform() {
        val html = """<video src="https://example.com/video.mp4"></video>"""
        val result = PatternManager.extractAndTransform(
            html,
            """src="([^"]+)"""",
            transform = { it.uppercase() }
        )
        
        assertEquals("HTTPS://EXAMPLE.COM/VIDEO.MP4", result)
    }

    @Test
    fun testExtractAndTransform_noMatch() {
        val html = """<div>No video</div>"""
        val result = PatternManager.extractAndTransform(
            html,
            """src="([^"]+)"""",
            transform = { it.uppercase() }
        )
        
        assertNull(result)
    }

    @Test
    fun testExtractUrl_absoluteUrl() {
        val html = """<a href="https://example.com/video.mp4">Video</a>"""
        val result = PatternManager.extractUrl(html, """href="([^"]+)"""")
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractUrl_protocolRelative() {
        val html = """<a href="//example.com/video.mp4">Video</a>"""
        val result = PatternManager.extractUrl(html, """href="([^"]+)"""")
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractUrl_absolutePath() {
        val html = """<a href="/videos/video.mp4">Video</a>"""
        val result = PatternManager.extractUrl(
            html,
            """href="([^"]+)"""",
            baseUrl = "https://example.com"
        )
        
        assertEquals("https://example.com/videos/video.mp4", result)
    }

    @Test
    fun testExtractUrl_relativePath() {
        val html = """<a href="videos/video.mp4">Video</a>"""
        val result = PatternManager.extractUrl(
            html,
            """href="([^"]+)"""",
            baseUrl = "https://example.com"
        )
        
        assertEquals("https://example.com/videos/video.mp4", result)
    }

    @Test
    fun testExtractUrl_baseUrlWithPath() {
        val html = """<a href="video.mp4">Video</a>"""
        val result = PatternManager.extractUrl(
            html,
            """href="([^"]+)"""",
            baseUrl = "https://example.com/path/to"
        )
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractUrl_noBaseUrl() {
        val html = """<a href="videos/video.mp4">Video</a>"""
        val result = PatternManager.extractUrl(html, """href="([^"]+)"""")
        
        assertEquals("videos/video.mp4", result)
    }
}
