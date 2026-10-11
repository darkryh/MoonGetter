package com.ead.lib.moongetter.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageAnalysisTest {

    @Test fun snapshotExtractsTitleScriptsAndCapsBody() {
        val snapshot = PageSnapshot(
            "https://a.com/x", "https://b.com/y", 200, mapOf("Content-Type" to "text/html"),
            """<html><head><TITLE>
                 Movie &amp; More - LuluStream </TITLE>
                 <script src="/js/xupload.js?v=2"></script><script type='text/javascript' src='https://cdn.x/p.js'></script>
                 <script>var src="/not-a-script-src.js"</script></head></html>""",
        )
        assertEquals("Movie & More - LuluStream", snapshot.title)
        assertEquals(listOf("/js/xupload.js?v=2", "https://cdn.x/p.js"), snapshot.scriptSources)
        assertEquals("text/html", snapshot.headers["content-type"])

        val capped = PageSnapshot("https://a.com", "https://a.com", 200, emptyMap(), "x".repeat(2_000), maxBodyChars = 1_024)
        assertEquals(1_024, capped.body.length)
        assertTrue(capped.isTruncated)
    }

    @Test fun signalsRespectTheirScope() {
        val snapshot = PageSnapshot("https://a.com", "https://a.com", 200, mapOf("Server" to "nginx-brand"),
            """<title>Brand Player</title><script src="/brand.js"></script><p>body-only</p>""")
        assertTrue(Signal.text("t", SignalKind.UNIQUE, SignalScope.TITLE, "brand player").matches(snapshot))
        assertFalse(Signal.text("t", SignalKind.UNIQUE, SignalScope.TITLE, "body-only").matches(snapshot))
        assertTrue(Signal.pattern("s", SignalKind.UNIQUE, SignalScope.SCRIPT_SRC, "^/brand\\.js$").matches(snapshot))
        assertFalse(Signal.pattern("s", SignalKind.UNIQUE, SignalScope.SCRIPT_SRC, "body-only").matches(snapshot))
        assertTrue(Signal.pattern("h", SignalKind.UNIQUE, SignalScope.HEADER, "server: nginx-brand").matches(snapshot))
        assertFalse(Signal.pattern("h", SignalKind.UNIQUE, SignalScope.HEADER, "brand player").matches(snapshot))
        assertTrue(Signal.text("b", SignalKind.UNIQUE, SignalScope.BODY, "BODY-ONLY").matches(snapshot))
        // Literal text is escaped, never interpreted as a pattern.
        assertFalse(Signal.text("b", SignalKind.UNIQUE, SignalScope.BODY, "b.dy-only").matches(snapshot))
    }

    @Test fun fingerprintAndFamilyValidateSignalKinds() {
        assertFailsWith<IllegalArgumentException> { Fingerprint(familyMarker("f", "x")) }
        assertFailsWith<IllegalArgumentException> { Fingerprint(unique("same", "a"), supporting("same", "b")) }
        assertFailsWith<IllegalArgumentException> { ServerFamily("f", listOf(unique("u", "x"))) }
        assertFailsWith<IllegalArgumentException> { ServerFamily("f", listOf(familyMarker("a", "x")), minimumHits = 2) }
        assertFailsWith<IllegalArgumentException> { Signal.text("t", SignalKind.UNIQUE, SignalScope.BODY, " ") }
    }

    @Test fun classifierRecognizesChallenges() {
        listOf(
            "<html><head><title>Just a moment...</title></head></html>",
            "<title>Attention Required! | Cloudflare</title>",
            "<div class=\"cf-chl-widget\"></div>",
            "<script>window._cf_chl_opt={cvId:'3'}</script>",
            "<title>DDoS-Guard</title>",
        ).forEach { assertEquals(PageKind.CHALLENGE, PageClassifier.classify(page(it, status = 403)), it) }
    }

    @Test fun cloudflareBeaconOnARealPlayerIsNotAChallenge() {
        val player = """<title>Movie - LuluStream</title><script>(function(){var s=document.createElement('script');
            s.src='/cdn-cgi/challenge-platform/scripts/precursor/main.js';})();</script><div id="vplayer"></div>"""
        assertEquals(PageKind.CONTENT, PageClassifier.classify(page(player)))
    }

    @Test fun classifierRecognizesParkedPages() {
        listOf(
            """<script>fetch("https://parklogic.com/",t)</script>""",
            "<h1>This domain is for sale</h1>",
            "<p>The domain may be for sale.</p>",
            "<a>Buy this domain</a>",
            "<title>x.com - Coming Soon</title><p>This domain is coming soon.</p>",
            "<title>katomen.store · budget-friendly .store listing | ED.com</title>",
        ).forEach { assertEquals(PageKind.PARKED, PageClassifier.classify(page(it)), it) }
    }

    @Test fun classifierRecognizesMissingFiles() {
        assertEquals(PageKind.NOT_FOUND, PageClassifier.classify(page("<html>gone</html>", status = 404)))
        assertEquals(PageKind.NOT_FOUND, PageClassifier.classify(page("<html>gone</html>", status = 410)))
        listOf(
            "<div>File is no longer available as it expired or has been deleted.</div>",
            "<h2>File was deleted</h2>",
            "<p>This video has been removed by the user.</p>",
            "<title>Video Not Found</title>",
        ).forEach { assertEquals(PageKind.NOT_FOUND, PageClassifier.classify(page(it)), it) }
    }

    @Test fun classifierPrecedenceAndEmptyPages() {
        assertEquals(PageKind.EMPTY, PageClassifier.classify(page("   \n ")))
        // A challenge served with 404 is still a challenge: its body is the protector's, not the host's.
        assertEquals(PageKind.CHALLENGE, PageClassifier.classify(page("<title>Just a moment...</title>", status = 404)))
        assertEquals(PageKind.PARKED, PageClassifier.classify(page("This domain is for sale", status = 404)))
        assertEquals(PageKind.CONTENT, PageClassifier.classify(page("<div id=vplayer>Not found in this sentence: video plays</div>")))
    }

    @Test fun navigationReadsLiteralTargetsOnly() {
        val html = """
            <meta http-equiv="refresh" content="0; url=/meta/target">
            <script>
              if (x) { window.location.href = 'https://next-mirror.com/e/abc'; }
              location.replace("/replaced");
              window.location.assign('https://assigned.com/e/1');
              window.location.href = currentUrl.toString();
              window.location.href = 'https://built.com/' + id;
              window.location.href = `https://template.com/${'$'}{id}`;
            </script>
            <iframe src="https://player.host.com/e/xyz" allowfullscreen></iframe>
            <iframe src="https://abysscdn.com/?v=${'$'}{slug}"></iframe>
            <iframe src='/relative/frame'></iframe>
        """.trimIndent()
        val found = PageNavigation.find(page(html, url = "https://wrapper.site/e/abc"))
        assertEquals(
            listOf(
                NavigationKind.SCRIPT_REDIRECT to "https://next-mirror.com/e/abc",
                NavigationKind.SCRIPT_REDIRECT to "https://wrapper.site/replaced",
                NavigationKind.SCRIPT_REDIRECT to "https://assigned.com/e/1",
                NavigationKind.META_REFRESH to "https://wrapper.site/meta/target",
                NavigationKind.FRAME to "https://player.host.com/e/xyz",
                NavigationKind.FRAME to "https://wrapper.site/relative/frame",
            ),
            found.map { it.kind to it.target.toString() },
        )
    }

    @Test fun urlSafetyRefusesInternalTargets() {
        listOf(
            "http://public.com/e/a", "https://public.com:8443/e/a", "https://10.0.0.1/e/a", "https://127.0.0.1/e/a",
            "https://localhost/e/a", "https://intranet/e/a", "https://db.internal/e/a", "https://nas.local/e/a",
            "https://router.home.arpa/e/a", "https://x.onion/e/a", "https://a.test/e/a",
        ).forEach { assertFalse(UrlSafety.isFetchable(EmbedUrl.parse(it)!!), it) }
        listOf("https://public.com/e/a", "https://public.com:443/e/a", "https://sub.mirror.xyz/e/a")
            .forEach { assertTrue(UrlSafety.isFetchable(EmbedUrl.parse(it)!!), it) }
    }
}
