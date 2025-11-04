package com.ead.lib.moongetter.utils

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommonPatternsTest {

    @Test
    fun testVideoPatterns_mp4Url() {
        val html = """<video src="https://example.com/video.mp4?token=abc123"></video>"""
        val match = Regex(CommonPatterns.Video.MP4_URL).find(html)
        
        assertNotNull(match)
        assertTrue(match.value.contains(".mp4"))
        assertTrue(match.value.contains("token=abc123"))
    }

    @Test
    fun testVideoPatterns_mp4UrlWithMultipleParams() {
        val html = """https://example.com/video.mp4?token=abc&quality=720p&auth=xyz"""
        val match = Regex(CommonPatterns.Video.MP4_URL).find(html)
        
        assertNotNull(match)
        assertTrue(match.value.contains("token=abc"))
        assertTrue(match.value.contains("quality=720p"))
        assertTrue(match.value.contains("auth=xyz"))
    }

    @Test
    fun testVideoPatterns_m3u8Url() {
        val html = """<source src="https://example.com/playlist.m3u8"></source>"""
        val match = Regex(CommonPatterns.Video.M3U8_URL).find(html)
        
        assertNotNull(match)
        assertTrue(match.value.contains(".m3u8"))
    }

    @Test
    fun testVideoPatterns_m3u8UrlWithParams() {
        val html = """https://example.com/playlist.m3u8?token=abc&quality=high"""
        val match = Regex(CommonPatterns.Video.M3U8_URL).find(html)
        
        assertNotNull(match)
        assertTrue(match.value.contains("token=abc"))
    }

    @Test
    fun testVideoPatterns_sourceTag() {
        val html = """<source src="https://example.com/video.mp4" type="video/mp4">"""
        val match = Regex(CommonPatterns.Video.SOURCE_TAG).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/video.mp4", match.groups[1]?.value)
    }

    @Test
    fun testVideoPatterns_videoTag() {
        val html = """<video src="https://example.com/video.mp4" controls></video>"""
        val match = Regex(CommonPatterns.Video.VIDEO_TAG).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/video.mp4", match.groups[1]?.value)
    }

    @Test
    fun testIFramePatterns_src() {
        val html = """<iframe src="https://example.com/embed/video" frameborder="0"></iframe>"""
        val match = Regex(CommonPatterns.IFrame.SRC).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/embed/video", match.groups[1]?.value)
    }

    @Test
    fun testIFramePatterns_dataSrc() {
        val html = """<iframe data-src="https://example.com/embed/video" loading="lazy"></iframe>"""
        val match = Regex(CommonPatterns.IFrame.DATA_SRC).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/embed/video", match.groups[1]?.value)
    }

    @Test
    fun testJavaScriptPatterns_packed() {
        val html = """<script>eval(function(p,a,c,k,e,d){return 'test'})</script>"""
        val match = Regex(CommonPatterns.JavaScript.PACKED).find(html)
        
        assertNotNull(match)
    }

    @Test
    fun testJavaScriptPatterns_variableAssignment() {
        val html = """var videoUrl = "https://example.com/video.mp4";"""
        val pattern = CommonPatterns.JavaScript.variableAssignment("videoUrl")
        val match = Regex(pattern).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/video.mp4", match.groups[1]?.value)
    }

    @Test
    fun testJavaScriptPatterns_jsonAssignment() {
        val html = """var config = {"url":"https://example.com/video.mp4"};"""
        val pattern = CommonPatterns.JavaScript.jsonAssignment("config")
        val match = Regex(pattern).find(html)
        
        assertNotNull(match)
        assertTrue(match.groups[1]?.value?.contains("url") ?: false)
    }

    @Test
    fun testUrlPatterns_domain() {
        val url = "https://example.com/path/to/video"
        val match = Regex(CommonPatterns.Url.DOMAIN).find(url)
        
        assertNotNull(match)
        assertEquals("example.com", match.groups[1]?.value)
    }

    @Test
    fun testUrlPatterns_queryParam() {
        val url = "https://example.com/video?quality=720p&token=abc123"
        val pattern = CommonPatterns.Url.queryParam("quality")
        val match = Regex(pattern).find(url)
        
        assertNotNull(match)
        assertEquals("720p", match.groups[1]?.value)
    }

    @Test
    fun testUrlPatterns_anyUrl() {
        val html = """Check out this video: https://example.com/video.mp4 it's great!"""
        val match = Regex(CommonPatterns.Url.ANY_URL).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/video.mp4", match.value)
    }

    @Test
    fun testAuthPatterns_token() {
        val html = """{"token": "abc123xyz"}"""
        val match = Regex(CommonPatterns.Auth.TOKEN).find(html)
        
        assertNotNull(match)
        assertEquals("abc123xyz", match.groups[1]?.value)
    }

    @Test
    fun testAuthPatterns_tokenVariants() {
        val variants = listOf(
            """token: "abc123"""",
            """token": "abc123"""",
            """token = "abc123"""",
            """token="abc123""""
        )
        
        variants.forEach { html ->
            val match = Regex(CommonPatterns.Auth.TOKEN).find(html)
            assertNotNull(match, "Failed to match: $html")
        }
    }

    @Test
    fun testAuthPatterns_apiKey() {
        val html = """api_key: "secret_key_123""""
        val match = Regex(CommonPatterns.Auth.API_KEY).find(html)
        
        assertNotNull(match)
        assertEquals("secret_key_123", match.groups[1]?.value)
    }

    @Test
    fun testAlternatives() {
        val pattern = CommonPatterns.alternatives(
            """\.mp4""",
            """\.m3u8""",
            """\.webm"""
        )
        
        listOf(".mp4", ".m3u8", ".webm").forEach { ext ->
            val match = Regex(pattern).find("video$ext")
            assertNotNull(match, "Failed to match: $ext")
        }
    }

    @Test
    fun testOptional() {
        val pattern = """url="([^"]+)"${CommonPatterns.optional("""\s+quality="([^"]+)"""")}"""
        
        // With quality
        val match1 = Regex(pattern).find("""url="video.mp4" quality="720p"""")
        assertNotNull(match1)
        
        // Without quality
        val match2 = Regex(pattern).find("""url="video.mp4"""")
        assertNotNull(match2)
    }

    @Test
    fun testBetween() {
        val pattern = CommonPatterns.between("<videoUrl>", "</videoUrl>")
        val html = """<videoUrl>https://example.com/video.mp4</videoUrl>"""
        val match = Regex(pattern).find(html)
        
        assertNotNull(match)
        assertEquals("https://example.com/video.mp4", match.groups[1]?.value)
    }

    @Test
    fun testBetween_customContent() {
        val pattern = CommonPatterns.between("<!--", "-->", content = """[^-]+""")
        val html = """<!-- This is a comment -->"""
        val match = Regex(pattern).find(html)
        
        assertNotNull(match)
        assertTrue(match.groups[1]?.value?.contains("comment") ?: false)
    }

    @Test
    fun testCombinedPatterns_realWorldScenario() {
        val html = """
            <video>
                <source src="https://example.com/video1.mp4" type="video/mp4">
                <source src="https://example.com/video2.m3u8" type="application/vnd.apple.mpegurl">
            </video>
        """.trimIndent()
        
        val combinedPattern = CommonPatterns.alternatives(
            CommonPatterns.Video.MP4_URL,
            CommonPatterns.Video.M3U8_URL
        )
        
        val matches = Regex(combinedPattern).findAll(html).toList()
        
        assertEquals(2, matches.size)
        assertTrue(matches[0].value.contains(".mp4"))
        assertTrue(matches[1].value.contains(".m3u8"))
    }
}
