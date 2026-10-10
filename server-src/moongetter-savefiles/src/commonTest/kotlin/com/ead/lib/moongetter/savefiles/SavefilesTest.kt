package com.ead.lib.moongetter.savefiles

import com.ead.lib.moongetter.savefiles.factory.SavefilesFactory
import kotlin.test.*

class SavefilesTest {
    @Test fun recognizesOnlySavefilesEmbeds() {
        val pattern = Regex(SavefilesFactory.pattern)
        assertTrue(pattern.matches("https://savefiles.com/e/riku8n8bps1j"))
        assertTrue(pattern.matches("https://www.savefiles.com/e/riku8n8bps1j/?poster=custom#play"))
        assertFalse(pattern.matches("https://savefiles.com.evil.test/e/riku8n8bps1j"))
        assertFalse(pattern.matches("https://other.test/?url=https://savefiles.com/e/riku8n8bps1j"))
        assertFalse(pattern.matches("https://savefiles.com/dl"))
    }

    @Test fun readsMultilinePlayerAndPreservesSignature() {
        val html = """jwplayer('vplayer').setup({
          sources: [
            {file:'https://cdn.example/master.m3u8?t=abc&amp;s=42'}
          ], image:'https://cdn.example/poster.jpg'
        });"""
        assertEquals("https://cdn.example/master.m3u8?t=abc&s=42", SavefilesPage.source(html))
    }

    @Test fun readsEscapedMediaAndRejectsThumbnails() {
        assertEquals("https://cdn.example/video.mp4", SavefilesPage.source("""sources:[{file:'https:\/\/cdn.example\/video.mp4'}]"""))
        assertNull(SavefilesPage.source("""tracks:[{file:'https://cdn.example/thumb.vtt'}],image:'https://cdn.example/image.jpg'"""))
        assertNull(SavefilesPage.source("""sources:[{file:'javascript:alert(1)'}]"""))
    }

    @Test fun recognizesVerificationWithoutInventingSource() {
        val challenge = """<title>Just a moment...</title><script src='/cdn-cgi/challenge-platform/main.js'></script>"""
        assertTrue(SavefilesPage.isChallenge(challenge))
        assertNull(SavefilesPage.source(challenge))
    }
}
