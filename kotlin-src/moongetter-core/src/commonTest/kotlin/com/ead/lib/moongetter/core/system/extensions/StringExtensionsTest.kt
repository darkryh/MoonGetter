package com.ead.lib.moongetter.core.system.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class StringExtensionsTest {

    @Test
    fun testDelete() {
        val text = "Hello World"
        val result = text.delete("World")
        
        assertEquals("Hello ", result)
    }

    @Test
    fun testReplaceDomainWith() {
        val url = "https://old-domain.com/path/to/video"
        val result = url.replaceDomainWith("new-domain.com")
        
        assertEquals("https://new-domain.com/path/to/video", result)
    }

    @Test
    fun testReplaceDomainWith_noMatch() {
        val text = "not a url"
        val result = text.replaceDomainWith("new-domain.com")
        
        assertNull(result)
    }

    @Test
    fun testExtractFirst() {
        val html = """<video src="https://example.com/video.mp4"></video>"""
        val result = html.extractFirst("""src="([^"]+)"""")
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractFirst_noMatch() {
        val html = """<div>No video</div>"""
        val result = html.extractFirst("""src="([^"]+)"""")
        
        assertNull(result)
    }

    @Test
    fun testExtractAll() {
        val html = """
            <source src="video1.mp4">
            <source src="video2.mp4">
            <source src="video3.mp4">
        """.trimIndent()
        
        val results = html.extractAll("""src="([^"]+)"""")
        
        assertEquals(3, results.size)
        assertEquals("video1.mp4", results[0])
        assertEquals("video2.mp4", results[1])
        assertEquals("video3.mp4", results[2])
    }

    @Test
    fun testExtractAll_noMatches() {
        val html = """<div>No videos</div>"""
        val results = html.extractAll("""src="([^"]+)"""")
        
        assertTrue(results.isEmpty())
    }

    @Test
    fun testExtractWithFallback_firstMatches() {
        val html = """var videoUrl = "https://example.com/video.mp4";"""
        val result = html.extractWithFallback(
            """videoUrl\s*=\s*"([^"]+)"""",
            """file:\s*'([^']+)'""",
            """src="([^"]+)""""
        )
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractWithFallback_secondMatches() {
        val html = """file: 'https://example.com/video.mp4'"""
        val result = html.extractWithFallback(
            """videoUrl\s*=\s*"([^"]+)"""",
            """file:\s*'([^']+)'""",
            """src="([^"]+)""""
        )
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractWithFallback_noneMatch() {
        val html = """<div>No video</div>"""
        val result = html.extractWithFallback(
            """videoUrl\s*=\s*"([^"]+)"""",
            """file:\s*'([^']+)'""",
            """src="([^"]+)""""
        )
        
        assertNull(result)
    }

    @Test
    fun testMatchesAny_matches() {
        val html = """<video src="test.mp4"></video>"""
        val result = html.matchesAny("""\.mp4""", """\.m3u8""", """\.webm""")
        
        assertTrue(result)
    }

    @Test
    fun testMatchesAny_noMatch() {
        val html = """<video src="test.avi"></video>"""
        val result = html.matchesAny("""\.mp4""", """\.m3u8""", """\.webm""")
        
        assertFalse(result)
    }

    @Test
    fun testExtractUrl_absolute() {
        val html = """<a href="https://example.com/video.mp4">Video</a>"""
        val result = html.extractUrl("""href="([^"]+)"""")
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractUrl_relative() {
        val html = """<a href="videos/video.mp4">Video</a>"""
        val result = html.extractUrl("""href="([^"]+)"""", baseUrl = "https://example.com")
        
        assertEquals("https://example.com/videos/video.mp4", result)
    }

    @Test
    fun testExtractGroups() {
        val html = """<video quality="720p" src="https://example.com/video.mp4"></video>"""
        val groups = html.extractGroups("""quality="([^"]+)"\s+src="([^"]+)"""")
        
        assertEquals(2, groups.size)
        assertEquals("720p", groups[0])
        assertEquals("https://example.com/video.mp4", groups[1])
    }

    @Test
    fun testExtractGroups_noMatch() {
        val html = """<div>No video</div>"""
        val groups = html.extractGroups("""src="([^"]+)"""")
        
        assertTrue(groups.isEmpty())
    }

    @Test
    fun testStripHtmlTags() {
        val html = """<p>This is <b>bold</b> text</p>"""
        val result = html.stripHtmlTags()
        
        assertEquals("This is bold text", result)
    }

    @Test
    fun testStripHtmlTags_noTags() {
        val text = "Plain text"
        val result = text.stripHtmlTags()
        
        assertEquals("Plain text", result)
    }

    @Test
    fun testDecodeHtmlEntities() {
        val html = """&lt;video&gt;&amp;&quot;&apos;&nbsp;&lt;/video&gt;"""
        val result = html.decodeHtmlEntities()
        
        assertEquals("""<video>&"' </video>""", result)
    }

    @Test
    fun testDecodeHtmlEntities_noEntities() {
        val text = "Plain text"
        val result = text.decodeHtmlEntities()
        
        assertEquals("Plain text", result)
    }

    @Test
    fun testExtractJsonValue() {
        val json = """{"videoUrl":"https://example.com/video.mp4","quality":"720p"}"""
        val result = json.extractJsonValue("videoUrl")
        
        assertEquals("https://example.com/video.mp4", result)
    }

    @Test
    fun testExtractJsonValue_notFound() {
        val json = """{"quality":"720p"}"""
        val result = json.extractJsonValue("videoUrl")
        
        assertNull(result)
    }

    @Test
    fun testHasVideoExtension_mp4() {
        val url = "https://example.com/video.mp4"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_m3u8() {
        val url = "https://example.com/playlist.m3u8"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_mpd() {
        val url = "https://example.com/manifest.mpd"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_avi() {
        val url = "https://example.com/video.avi"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_mkv() {
        val url = "https://example.com/video.mkv"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_webm() {
        val url = "https://example.com/video.webm"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_caseInsensitive() {
        val url = "https://example.com/video.MP4"
        assertTrue(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_notVideo() {
        val url = "https://example.com/page.html"
        assertFalse(url.hasVideoExtension())
    }

    @Test
    fun testHasVideoExtension_withQueryParams() {
        val url = "https://example.com/video.mp4?token=abc123"
        assertTrue(url.hasVideoExtension())
    }
}
