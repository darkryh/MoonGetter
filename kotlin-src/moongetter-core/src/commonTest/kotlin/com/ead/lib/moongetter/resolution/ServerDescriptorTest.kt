package com.ead.lib.moongetter.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerDescriptorTest {

    private val hosts = HostSet(
        exact = listOf("voe.sx", "player.example-cdn.com"),
        subdomainsOf = listOf("rpmstream.live"),
        families = listOf("""voeunblock(?:[1-9]|10)?\.com"""),
    )

    private val descriptor = ServerDescriptor(
        hosts = hosts,
        paths = listOf(
            PathRule("""/(?:e|d)/(\w+)/?(?:[?#]\S*)?"""),
            PathRule("""/?#([A-Za-z0-9_-]{2,64})(?:&\S*)?""", prior = PathRule.DISTINCTIVE_PRIOR),
        ),
    )

    @Test fun hostSetMatchesWholeHostnamesOnly() {
        listOf("voe.sx", "www.voe.sx", "VOE.SX", "voe.sx.", "player.example-cdn.com", "rpmstream.live", "gf21.rpmstream.live",
            "a.b.rpmstream.live", "voeunblock.com", "voeunblock1.com", "voeunblock10.com").forEach {
            assertTrue(hosts.contains(it), it)
        }
        listOf("voe.sx.evil.test", "evilvoe.sx", "voe.sxx", "xvoe.sx", "www.www.voe.sx", "example-cdn.com", "rpmstream.live.evil",
            "evilrpmstream.live", "voeunblock11.com", "voeunblock0.com", "voeunblock1.com.evil", "voeunblockx.com").forEach {
            assertFalse(hosts.contains(it), it)
        }
    }

    @Test fun hostSetRejectsInvalidConfiguration() {
        assertFailsWith<IllegalArgumentException> { HostSet() }
        assertFailsWith<IllegalArgumentException> { HostSet(exact = listOf("bad host")) }
        assertFailsWith<IllegalArgumentException> { ServerDescriptor(hosts, paths = emptyList()) }
        assertFailsWith<IllegalArgumentException> { PathRule("/e/(x)", prior = -1) }
    }

    @Test fun matchesKnownHostsAndExtractsIds() {
        assertEquals("abc123", descriptor.match("https://voe.sx/e/abc123")?.id)
        assertEquals("abc123", descriptor.match("https://www.voe.sx/d/abc123/?autoplay=1#t=3")?.id)
        assertEquals("w8xvpz", descriptor.match("https://gf21.rpmstream.live/#w8xvpz&poster=x")?.id)
        assertEquals("w8xvpz", descriptor.match("https://rpmstream.live#w8xvpz")?.id)
        assertEquals(PathRule.DISTINCTIVE_PRIOR, descriptor.match("https://rpmstream.live/#w8xvpz")?.rule?.prior)
    }

    @Test fun rejectsWrongSchemePortHostOrShape() {
        listOf(
            "http://voe.sx/e/abc123",
            "https://voe.sx:8443/e/abc123",
            "https://voe.sx/x/abc123",
            "https://voe.sx/e/",
            "https://voe.sx/e/abc123/extra",
            "https://voe.sx.evil.test/e/abc123",
            "https://evil.test/e/abc123?u=https://voe.sx/e/abc123",
            "https://evil.test/https://voe.sx/e/abc123",
            "https://voe.sx@evil.test/e/abc123",
            "https://evil.test#https://voe.sx/e/abc123",
            "https://rpmstream.live/#a",
            "https://rpmstream.live/#abc/123",
        ).forEach { assertNull(descriptor.match(it), it) }
    }

    @Test fun matchPathIgnoresTheHost() {
        assertEquals("abc123", descriptor.matchPath("https://unlisted-mirror.xyz/e/abc123")?.id)
        assertNull(descriptor.matchPath("http://unlisted-mirror.xyz/e/abc123"))
        assertNull(descriptor.matchPath("https://unlisted-mirror.xyz/watch/abc123"))
    }

    @Test fun allowHttpOnlyWhenDeclared() {
        val permissive = ServerDescriptor(hosts, descriptor.paths, allowHttp = true)
        assertNotNull(permissive.match("http://voe.sx/e/abc123"))
        assertNotNull(permissive.match("http://voe.sx:80/e/abc123"))
        assertNull(permissive.match("ftp://voe.sx/e/abc123"))
    }

    @Test fun derivedPatternIsEquivalentToMatch() {
        val corpus = buildList {
            val hostsToTry = listOf("voe.sx", "www.voe.sx", "VOE.sx", "voe.sx.", "gf21.rpmstream.live", "rpmstream.live", "voeunblock7.com",
                "voe.sx.evil.test", "evilvoe.sx", "voeunblock11.com", "player.example-cdn.com", "example-cdn.com")
            val rests = listOf("/e/abc123", "/d/abc123/", "/e/abc123?x=1", "/e/abc123#t", "/#w8xvpz", "#w8xvpz", "/#w8xvpz&a=b",
                "/e/", "/x/abc", "/e/abc/extra", "/#a", "")
            for (scheme in listOf("https", "http")) for (host in hostsToTry) for (port in listOf("", ":443", ":8443")) for (rest in rests) {
                add("$scheme://$host$port$rest")
            }
        }
        for (candidate in listOf(descriptor, ServerDescriptor(hosts, descriptor.paths, allowHttp = true))) {
            val regex = Regex(candidate.pattern)
            corpus.forEach { url ->
                assertEquals(candidate.match(url) != null, regex.matches(url), "pattern/descriptor disagree on $url")
            }
        }
    }
}
