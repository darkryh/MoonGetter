package com.ead.lib.moongetter.vimeos

import com.ead.lib.moongetter.js.unpacker.JsUnpacker
import com.ead.lib.moongetter.vimeos.factory.VimeosFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VimeosTest {
    @Test fun matchesOnlyVimeosEmbedPages() {
        val pattern = Regex(VimeosFactory.pattern)
        assertTrue(pattern.matches("https://vimeos.net/embed-2nulhg4mnuz7.html"))
        assertTrue(pattern.matches("https://www.vimeos.net/embed-2nulhg4mnuz7.html?autoplay=1"))
        assertFalse(pattern.matches("https://vimeo.com/embed-2nulhg4mnuz7.html"))
        assertFalse(pattern.matches("https://vimeos.net.evil.test/embed-2nulhg4mnuz7.html"))
    }

    @Test fun unpacksPlayerMediaSource() {
        val packed = """eval(function(p,a,c,k,e,d){return p}('0:[{1:"2://3.4/5.6?7=8"}]',36,9,'sources|file|https|cdn|example|master|m3u8|token|abc'.split('|'),0,{}))"""
        val unpacked = JsUnpacker.unpackAndCombine(packed)!!
        assertTrue(unpacked.contains("sources:[{file:"))
        assertEquals("https://cdn.example/master.m3u8?token=abc",
            Regex("""(https://[^\s"']+\.m3u8(?:\?[^\s"']*)?)""").find(unpacked)?.value)
    }
}
