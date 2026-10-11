package com.ead.lib.moongetter.server.bundle

import com.ead.lib.moongetter.resolution.Resolution
import com.ead.lib.moongetter.resolution.ResolutionConfig
import com.ead.lib.moongetter.resolution.ResolutionStage
import com.ead.lib.moongetter.resolution.UnresolvedReason
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.fixtures
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Near-duplicate pages built from real fixtures: branding removed, branding swapped, markers from two
 * services mixed, real players wrapped in interstitials. These are the cases where similar services
 * could be confused, and the resolver must either pick the right one or refuse.
 */
class SimilarityStressTest {

    private val url = "https://unlisted-stress.xyz/e/abc123def"
    private val config = ResolutionConfig(fallbackEnabled = true, learnedHosts = null)

    private fun html(file: String) = fixtures.single { it.file == file }.html

    /** A page edit that must change the page; a silent no-op would make the test meaningless. */
    private fun String.swap(old: String, new: String) = replace(old, new).also { check(it != this) { "'$old' not found in page" } }
    private fun String.swap(old: Regex, new: String) = replace(old, new).also { check(it != this) { "$old not found in page" } }

    private fun resolve(body: String, status: Int = 200, at: String = url): Resolution = runBlocking {
        BundleTestSupport.resolver(config).resolve(at, PageClient(mapOf(at to (status to body))), emptyMap())
    }

    private fun Resolution.server() = (this as? Resolution.Resolved)?.primary?.factory?.serverName
    private fun Resolution.candidates() = (this as Resolution.Resolved).candidates.map { it.factory.serverName }

    private val luluTitle = Regex("""<title>[\s\S]*?</title>""", RegexOption.IGNORE_CASE)

    // ---- XFileSharing siblings: Streamwish, Vidhide, Lulustream ----

    @Test fun genericXfsPageAddsLulustreamBrandingAndBecomesLulustream() {
        val body = html("streamwish-1.html").swap(luluTitle, "<title>Some.Movie.mkv - LuluStream</title>")
        val resolution = resolve(body)
        assertEquals(ResolutionStage.FINGERPRINT, (resolution as Resolution.Resolved).stage)
        assertEquals("Lulustream", resolution.server())
    }

    @Test fun genericXfsPageAddsVidhideAssetsAndBecomesVidhide() {
        val body = html("streamwish-1.html").swap("""href="/css/main.css"""", """href="/css/main.css"><link rel="icon" href="/vidhide/images/favicon/favicon.ico"""")
        assertEquals("Vihide", resolve(body).server())
    }

    @Test fun xfsPageWithBothSiblingBrandsIsAmbiguous() {
        val body = html("streamwish-1.html")
            .swap(luluTitle, "<title>Movie - LuluStream</title>")
            .swap("""href="/css/main.css"""", """href="/css/main.css"><link rel="icon" href="/vidhide/images/favicon/favicon.ico"""")
        val unresolved = assertIs<Resolution.Unresolved>(resolve(body))
        assertEquals(UnresolvedReason.AMBIGUOUS, unresolved.reason)
        assertEquals(setOf("Lulustream", "Vihide"), unresolved.suspects.toSet())
    }

    @Test fun lulustreamWithoutItsTitleFallsBackToFamilyProbingWithLulustreamFirst() {
        // The CDN reference still supports Lulustream, so it is tried first, but nothing is claimed outright.
        val body = html("lulustream-2.html").swap(luluTitle, "<title>Movie.mkv</title>")
        val resolution = assertIs<Resolution.Resolved>(resolve(body))
        assertEquals(ResolutionStage.FAMILY_PROBE, resolution.stage)
        assertEquals("Lulustream", resolution.candidates().first())
        assertEquals(setOf("Lulustream", "Streamwish", "Vihide"), resolution.candidates().toSet())
    }

    @Test fun vidhideWithoutItsAssetPathFallsBackToFamilyProbingWithVidhideFirst() {
        val body = html("vidhide-1.html").swap("/vidhide/images/", "/assets/images/")
        val resolution = assertIs<Resolution.Resolved>(resolve(body))
        assertEquals(ResolutionStage.FAMILY_PROBE, resolution.stage)
        assertEquals("Vihide", resolution.candidates().first())
    }

    @Test fun unbrandedXfsPagesTryTheUnbrandedMemberFirst() {
        val resolution = assertIs<Resolution.Resolved>(resolve(html("streamwish-1.html")))
        assertEquals(listOf("Streamwish", "Lulustream", "Vihide"), resolution.candidates())
    }

    @Test fun xfsPageMissingMostFamilyMarkersIsNotGuessed() {
        val body = html("streamwish-1.html")
            .swap(Regex("""eval\(function\(p,a,c,k,e,d\)"""), "eval(function(q)")
            .swap("/js/xupload.js", "/js/upload.js")
            .swap("vplayer", "player")
        assertEquals(UnresolvedReason.NO_EVIDENCE, (resolve(body) as Resolution.Unresolved).reason)
    }

    @Test fun deletedXfsPageWithBrandingIsStillNotFound() {
        val body = html("vidhide-2.html").swap("<html>", "<html><title>x - LuluStream</title>")
        assertEquals(UnresolvedReason.NOT_FOUND, (resolve(body) as Resolution.Unresolved).reason)
    }

    // ---- Different services mixed on one page ----

    @Test fun mixdropPageWithAStreamtapePlayerSnippetIsAmbiguous() {
        val body = html("mixdrop-1.html").swap("</body>", """<div id="robotlink" style="display:none;">//streamtape.com/get_video?id=1</div></body>""")
        val unresolved = assertIs<Resolution.Unresolved>(resolve(body))
        assertEquals(UnresolvedReason.AMBIGUOUS, unresolved.reason)
        assertEquals(setOf("Mixdrop", "Streamtape"), unresolved.suspects.toSet())
    }

    @Test fun voePlayerEmbeddingAbyssScriptsIsAmbiguous() {
        val body = html("voe-2.html").swap("</head>", """<script src="https://iamcdn.net/player-v2/core.bundle.js"></script></head>""")
        assertEquals(UnresolvedReason.AMBIGUOUS, (resolve(body) as Resolution.Unresolved).reason)
    }

    @Test fun xfsPageWithAMixdropMarkerIsNotForcedIntoTheFamily() {
        // Mixdrop's unique marker (50) leads the family members (40 + path) by less than the margin.
        val body = html("streamwish-1.html") + "<script>MDCore.ref = \"abc\";</script>"
        val resolution = resolve(body)
        assertTrue(resolution is Resolution.Unresolved, "got $resolution")
        assertEquals(UnresolvedReason.AMBIGUOUS, resolution.reason)
    }

    // ---- Weakened real players ----

    @Test fun voeWithoutItsBrandedTitleIsNotGuessed() {
        // Only supporting markers remain (loader script, encoded config): consistent with Voe, but not proof.
        val body = html("voe-2.html").swap(Regex("""<title>[\s\S]*?</title>"""), "<title>Watch</title>")
        val unresolved = assertIs<Resolution.Unresolved>(resolve(body))
        assertEquals(UnresolvedReason.NO_EVIDENCE, unresolved.reason)
        assertEquals(listOf("Voe"), unresolved.suspects)
    }

    @Test fun abyssStillResolvesWithOneOfItsTwoUniqueMarkers() {
        val withoutBundle = html("abyss-2.html").swap("player-v2/core.bundle.js", "player-v2/app.js")
        assertEquals("Abyss", resolve(withoutBundle).server())
        val withoutGlobal = html("abyss-2.html").swap("SoTrym", "PlayerData")
        assertEquals("Abyss", resolve(withoutGlobal).server())
    }

    @Test fun rpmShellOnItsFragmentPathIsStillOnlyASuspect() {
        val resolution = resolve(html("rpmstream-1.html"), at = "https://unlisted-stress.xyz/#w8xvpz")
        val unresolved = assertIs<Resolution.Unresolved>(resolution)
        assertEquals(listOf("RPMStream"), unresolved.suspects)
    }

    // ---- Interstitials and placeholders wrapping real players ----

    @Test fun realPlayerBehindAChallengeIsAChallenge() {
        val body = html("abyss-2.html").swap(Regex("""<title>[\s\S]*?</title>"""), "<title>Just a moment...</title>")
        assertEquals(UnresolvedReason.CHALLENGE, (resolve(body, status = 403) as Resolution.Unresolved).reason)
    }

    @Test fun parkedPageQuotingAServiceNameIsParked() {
        val body = html("parked-for-sale.html").swap("</body>", "<p>Previously used by VOE | Content Delivery Network</p></body>")
        assertEquals(UnresolvedReason.PARKED, (resolve(body) as Resolution.Unresolved).reason)
    }

    @Test fun cloudflareBeaconsOnRealPlayersDoNotHideThem() {
        // lulustream-2 includes Cloudflare's challenge-platform beacon script and must still resolve.
        assertTrue(html("lulustream-2.html").contains("challenge-platform"))
        assertEquals("Lulustream", resolve(html("lulustream-2.html")).server())
    }

    // ---- Navigation ----

    @Test fun wrapperFramingAnUnknownHostIsNotTreatedAsAFrameMatch() {
        val body = html("filemoon-2.html").swap("https://bysekoze.com/", "https://unlisted-player.xyz/")
        val resolution = resolve(body)
        assertTrue(resolution is Resolution.Unresolved, "got $resolution")
    }

    @Test fun scriptRedirectToAnUnknownMirrorIsFollowedToTheRealPlayer() {
        val start = "https://unlisted-entry.xyz/e/5dunxepcpd42"
        val next = "https://unlisted-next.xyz/e/5dunxepcpd42"
        val redirect = html("voe-1.html").swap("https://megankiddifference.com/e/5dunxepcpd42", next)
        val pages = mapOf(start to (200 to redirect), next to (200 to html("voe-2.html")))
        val client = PageClient(pages)
        val resolution = runBlocking { BundleTestSupport.resolver(config).resolve(start, client, emptyMap()) }
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.FINGERPRINT, resolved.stage)
        assertEquals("Voe", resolved.server())
        assertEquals(next, resolved.primary.url)
        assertEquals(listOf(start, next), client.requests)
    }

    // ---- Order independence ----

    @Test fun decisionsDoNotDependOnEngineOrderExceptFamilyTies() {
        val forward = BundleTestSupport.factories
        val backward = forward.reversed()
        fixtures.forEach { fixture ->
            val a = runBlocking {
                com.ead.lib.moongetter.resolution.ServerResolver(forward, config)
                    .resolve(fixture.url, PageClient(mapOf(fixture.url to (fixture.status to fixture.html))), emptyMap())
            }
            val b = runBlocking {
                com.ead.lib.moongetter.resolution.ServerResolver(backward, config)
                    .resolve(fixture.url, PageClient(mapOf(fixture.url to (fixture.status to fixture.html))), emptyMap())
            }
            assertEquals(a::class, b::class, fixture.file)
            when (a) {
                is Resolution.Resolved -> {
                    b as Resolution.Resolved
                    assertEquals(a.stage, b.stage, fixture.file)
                    assertEquals(a.candidates.map { it.factory.serverName }.toSet(), b.candidates.map { it.factory.serverName }.toSet(), fixture.file)
                    if (a.stage != ResolutionStage.FAMILY_PROBE) assertEquals(a.server(), b.server(), fixture.file)
                }
                is Resolution.Unresolved -> assertEquals(a.reason, (b as Resolution.Unresolved).reason, fixture.file)
            }
        }
    }
}
