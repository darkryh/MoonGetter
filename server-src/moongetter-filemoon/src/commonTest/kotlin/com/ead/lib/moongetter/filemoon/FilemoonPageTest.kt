package com.ead.lib.moongetter.filemoon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FilemoonPageTest {
    @Test fun extractsDirectPackedPlayer() {
        val html = """<script>eval(function(p,a,c,k,e,d){return p}('0:[{1:"2://3.4/5.6?7=8"}]',36,9,'sources|file|https|cdn|example|master|m3u8|token|abc'.split('|'),0,{}))</script>"""
        assertEquals("https://cdn.example/master.m3u8?token=abc", FilemoonPage.source(html))
    }
    @Test fun acceptsMultilineRelativeIframe() {
        assertEquals("/player/abc", FilemoonPage.iframe("""<IFRAME
            allowfullscreen src = '/player/abc'></IFRAME>"""))
    }
    @Test fun identifiesProtectedFrontendWithoutInventingSource() {
        val html = """<title>Byse Frontend</title><div id="root"></div>"""
        assertTrue(FilemoonPage.isByse(html))
        assertNull(FilemoonPage.source(html))
        assertNull(FilemoonPage.iframe(html))
    }
}
