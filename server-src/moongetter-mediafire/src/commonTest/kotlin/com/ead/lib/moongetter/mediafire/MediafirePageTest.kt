package com.ead.lib.moongetter.mediafire

import kotlin.test.*
import kotlin.io.encoding.Base64

class MediafirePageTest {
    @Test fun directButtonAcrossLines() {
        assertEquals("https://download1.mediafire.com/file.mp4?a=1&b=2", MediafirePage.source("""<a href='https://download1.mediafire.com/file.mp4?a=1&amp;b=2'
            id='downloadButton' aria-label='Download file'>"""))
    }
    @Test fun legacyScrambledButton() {
        val url = "https://download1.mediafire.com/file.mp4"
        assertEquals(url, MediafirePage.source("""<a data-scrambled-url='${Base64.encode(url.encodeToByteArray())}' aria-label='Download file'>"""))
    }
    @Test fun rejectsAdsAndMalformedSources() {
        assertNull(MediafirePage.source("""<a href='https://ads.example/video.mp4'>"""))
        assertNull(MediafirePage.source("""<a id='downloadButton' href='https://mediafire.com.evil.example/a'>"""))
        assertNull(MediafirePage.source("""<a id='downloadButton' data-scrambled-url='!invalid!'>"""))
    }
}
