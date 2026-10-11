package com.ead.lib.moongetter.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmbedUrlTest {

    @Test fun parsesAndNormalizesHost() {
        val url = assertNotNull(EmbedUrl.parse("HTTPS://WWW.Voe.SX./e/AbC123?x=1#frag"))
        assertEquals("https", url.scheme)
        assertEquals("www.voe.sx", url.host)
        assertEquals("voe.sx", url.hostWithoutWww)
        assertEquals("/e/AbC123?x=1#frag", url.rest)
        assertEquals("/e/AbC123", url.path)
        assertEquals(null, url.port)
        assertTrue(url.usesDefaultPort)
    }

    @Test fun keepsPathCaseAndEmptyRest() {
        assertEquals("/E/MiXeD", EmbedUrl.parse("https://a.com/E/MiXeD")?.rest)
        assertEquals("", EmbedUrl.parse("https://a.com")?.rest)
        assertEquals("#abc", EmbedUrl.parse("https://rpmstream.live#abc")?.rest)
        assertEquals("?v=abc", EmbedUrl.parse("https://abyss.to?v=abc")?.rest)
    }

    @Test fun parsesPorts() {
        assertEquals(443, EmbedUrl.parse("https://a.com:443/x")?.port)
        assertTrue(EmbedUrl.parse("https://a.com:443/x")!!.usesDefaultPort)
        assertEquals(false, EmbedUrl.parse("https://a.com:8443/x")?.usesDefaultPort)
        assertTrue(EmbedUrl.parse("http://a.com:80/x")!!.usesDefaultPort)
    }

    @Test fun rejectsMalformedAndDeceptiveInput() {
        listOf(
            "",
            "voe.sx/e/abc",
            "//voe.sx/e/abc",
            "https://",
            "https:///e/abc",
            "https://voe.sx@evil.test/e/abc",
            "https://user:pass@voe.sx/e/abc",
            "https://voe .sx/e/abc",
            "https://voe.sx/e/abc def",
            "https://voe.sx/e/abc\n",
            "https://[::1]/e/abc",
            "https://vоe.sx/e/abc", // Cyrillic о
            "https://voe..sx/e/abc",
            "https://-voe.sx/e/abc",
            "https://voe-.sx/e/abc",
            "https://voe.sx:/e/abc",
            "https://voe.sx:0/e/abc",
            "https://voe.sx:65536/e/abc",
            "https://voe.sx:44a/e/abc",
            "https://voe.sxe/abc".replace("sxe", "sx\u0000e"),
            "https://${"a".repeat(64)}.com/e/abc",
        ).forEach { assertNull(EmbedUrl.parse(it), it) }
    }

    @Test fun recognizesIpLiterals() {
        assertTrue(EmbedUrl.parse("https://127.0.0.1/x")!!.isIpLiteral)
        assertEquals(false, EmbedUrl.parse("https://1.example.com/x")!!.isIpLiteral)
    }

    @Test fun resolvesReferences() {
        val base = EmbedUrl.parse("https://mirror.xyz/e/abc?x=1#f")!!
        assertEquals("https://other.com/e/1", base.resolve("https://other.com/e/1").toString())
        assertEquals("https://cdn.com/e/1", base.resolve("//cdn.com/e/1").toString())
        assertEquals("https://mirror.xyz/v/2", base.resolve("/v/2").toString())
        assertEquals("https://mirror.xyz/e/abc?y=2", base.resolve("?y=2").toString())
        assertEquals("https://mirror.xyz/e/abc?x=1#g", base.resolve("#g").toString())
        assertEquals("https://mirror.xyz/e/next", base.resolve("next").toString())
        assertNull(base.resolve(""))
        assertNull(base.resolve("javascript alert(1)"))
    }

    @Test fun equalityUsesNormalizedForm() {
        assertEquals(EmbedUrl.parse("https://VOE.sx/e/a"), EmbedUrl.parse("https://voe.sx/e/a"))
    }
}
