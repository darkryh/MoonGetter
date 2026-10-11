package com.ead.lib.moongetter.resolution

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerResolverTest {

    private val alpha = fakeFactory("Alpha", listOf("alpha.com"), listOf(unique("alpha.unique", "alpha-unique")))
    private val beta = fakeFactory("Beta", listOf("beta.com"), listOf(unique("beta.unique", "beta-unique")))
    private val x1 = fakeFactory("X1", listOf("x1.com"), family = TestFamily)
    private val x2 = fakeFactory("X2", listOf("x2.com"), family = TestFamily)
    private val factories = listOf(alpha, beta, x1, x2)
    private val enabled = ResolutionConfig(fallbackEnabled = true)
    private val unknown = "https://unknown-mirror.xyz/e/abc123"

    private fun resolve(url: String, pages: Map<String, FakePage>, config: ResolutionConfig = enabled, list: List<com.ead.lib.moongetter.models.Server.Factory> = factories) =
        runBlocking {
            val client = FakeClient(pages)
            ServerResolver(list, config).resolve(url, client, mapOf("User-Agent" to "test")) to client
        }

    // ---- Stage 1: known hosts ----

    @Test fun knownHostResolvesWithoutNetwork() {
        val (resolution, client) = resolve("https://www.alpha.com/e/abc123", emptyMap())
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.KNOWN_HOST, resolved.stage)
        assertEquals("Alpha", resolved.primary.factory.serverName)
        assertTrue(client.requests.isEmpty())
    }

    @Test fun twoDescriptorsClaimingAUrlIsAConflictNotAGuess() {
        val clone = fakeFactory("AlphaClone", listOf("alpha.com"))
        val resolution = ServerResolver(listOf(alpha, clone)).resolveStatic("https://alpha.com/e/abc123")
        val unresolved = assertIs<Resolution.Unresolved>(resolution)
        assertEquals(UnresolvedReason.CONFLICTING_SERVERS, unresolved.reason)
        assertEquals(listOf("Alpha", "AlphaClone"), unresolved.suspects)
    }

    @Test fun legacyPatternsKeepTheirSemanticsAndLoseToDescriptors() {
        val legacy = FakeFactory("Legacy", descriptor = null, legacyPattern = """legacy\.com/v/\d+""")
        val greedy = FakeFactory("Greedy", descriptor = null, legacyPattern = """alpha""")
        val resolver = ServerResolver(listOf(legacy, alpha, greedy))
        assertEquals("Legacy", (resolver.resolveStatic("https://legacy.com/v/12") as Resolution.Resolved).primary.factory.serverName)
        // The descriptor match is exact, so a sloppy legacy pattern that also matches does not create a conflict.
        assertEquals("Alpha", (resolver.resolveStatic("https://alpha.com/e/abc123") as Resolution.Resolved).primary.factory.serverName)
        val legacyConflict = ServerResolver(listOf(legacy, FakeFactory("Legacy2", null, """legacy\.com"""))).resolveStatic("https://legacy.com/v/12")
        assertEquals(UnresolvedReason.CONFLICTING_SERVERS, (legacyConflict as Resolution.Unresolved).reason)
    }

    @Test fun invalidUrlsAreReported() {
        assertEquals(UnresolvedReason.INVALID_URL, (ServerResolver(factories).resolveStatic("not a url") as Resolution.Unresolved).reason)
    }

    // ---- Fallback gating and safety ----

    @Test fun disabledFallbackNeverTouchesTheNetwork() {
        val (resolution, client) = resolve(unknown, emptyMap(), config = ResolutionConfig(fallbackEnabled = false))
        assertEquals(UnresolvedReason.NO_KNOWN_HOST, (resolution as Resolution.Unresolved).reason)
        assertTrue(client.requests.isEmpty())
    }

    @Test fun unsafeUrlsAreNeverFetched() {
        listOf("http://unknown-mirror.xyz/e/abc123", "https://127.0.0.1/e/abc123", "https://localhost/e/abc123",
            "https://unknown-mirror.xyz:8443/e/abc123", "https://nas.local/e/abc123").forEach { url ->
            val (resolution, client) = resolve(url, emptyMap())
            assertEquals(UnresolvedReason.UNSAFE_URL, (resolution as Resolution.Unresolved).reason, url)
            assertTrue(client.requests.isEmpty(), url)
        }
    }

    @Test fun redirectToAnInternalHostIsRefused() {
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("alpha-unique", finalUrl = "https://10.1.2.3/e/abc123")))
        assertEquals(UnresolvedReason.UNSAFE_URL, (resolution as Resolution.Unresolved).reason)
    }

    @Test fun fetchFailuresAreReportedAndCancellationPropagates() {
        val (resolution, _) = resolve(unknown, emptyMap())
        assertEquals(UnresolvedReason.FETCH_FAILED, (resolution as Resolution.Unresolved).reason)

        val cancelling = object : com.ead.lib.moongetter.client.MoonClient {
            override fun initConfigurationData(configData: com.ead.lib.moongetter.client.models.Configuration.Data) = Unit
            override suspend fun <T> request(request: com.ead.lib.moongetter.client.request.Request<T>) = throw CancellationException("stop")
        }
        assertFailsWith<CancellationException> { runBlocking { ServerResolver(factories, enabled).resolve(unknown, cancelling, emptyMap()) } }
    }

    // ---- Stage 3: redirects ----

    @Test fun httpRedirectToAKnownHostIsLearnable() {
        val (resolution, client) = resolve(unknown, mapOf(unknown to FakePage("<html></html>", finalUrl = "https://alpha.com/e/abc123")))
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.REDIRECT, resolved.stage)
        assertEquals("https://alpha.com/e/abc123", resolved.primary.url)
        assertEquals("unknown-mirror.xyz", resolved.learnableHost)
        assertEquals(1, client.requests.size)
        assertEquals("test", client.requests.single().headers["User-Agent"])
    }

    @Test fun scriptRedirectToAKnownHostIsUsedButNotLearned() {
        val html = """<title>Redirecting...</title><script>window.location.href = 'https://beta.com/e/abc123';</script>"""
        val (resolution, client) = resolve(unknown, mapOf(unknown to FakePage(html)))
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.REDIRECT, resolved.stage)
        assertEquals("Beta", resolved.primary.factory.serverName)
        assertNull(resolved.learnableHost)
        assertEquals(1, client.requests.size)
    }

    @Test fun unknownScriptRedirectChainsAreFollowedWithinTheHopLimit() {
        val hop1 = "https://hop-one.xyz/e/abc123"
        val hop2 = "https://hop-two.xyz/e/abc123"
        val pages = mapOf(
            unknown to FakePage("<script>location.replace('$hop1')</script>"),
            hop1 to FakePage("<script>window.location = '$hop2';</script>"),
            hop2 to FakePage("<p>alpha-unique</p>"),
        )
        val (resolution, client) = resolve(unknown, pages)
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.FINGERPRINT, resolved.stage)
        assertEquals(hop2, resolved.primary.url)
        assertNull(resolved.learnableHost, "a host reached through script hops is not learned")
        assertEquals(listOf(unknown, hop1, hop2), client.requestedUrls)

        val (limited, limitedClient) = resolve(unknown, pages, ResolutionConfig(fallbackEnabled = true, maxNavigationHops = 2))
        assertEquals(UnresolvedReason.NO_EVIDENCE, (limited as Resolution.Unresolved).reason)
        assertEquals(2, limitedClient.requests.size)
    }

    @Test fun redirectLoopsStop() {
        val other = "https://loop.xyz/e/abc123"
        val pages = mapOf(
            unknown to FakePage("<script>window.location.href = '$other';</script>"),
            other to FakePage("<script>window.location.href = '$unknown';</script>"),
        )
        val (resolution, client) = resolve(unknown, pages)
        assertEquals(UnresolvedReason.NO_EVIDENCE, (resolution as Resolution.Unresolved).reason)
        assertEquals(2, client.requests.size)
    }

    // ---- Stage 4: frames ----

    @Test fun wrapperPageWithAKnownFrameUsesTheFrame() {
        val html = """<html><body><iframe src="https://alpha.com/e/inner99" allowfullscreen></iframe></body></html>"""
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage(html)))
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.EMBEDDED_FRAME, resolved.stage)
        assertEquals("https://alpha.com/e/inner99", resolved.primary.url)
        assertNull(resolved.learnableHost, "a wrapper host is not the server's host")
    }

    // ---- Stage 5: page state, fingerprints and families ----

    @Test fun pageStatesStopBeforeScoring() {
        mapOf(
            FakePage("<title>Just a moment...</title> alpha-unique", status = 403) to UnresolvedReason.CHALLENGE,
            FakePage("This domain is for sale. alpha-unique") to UnresolvedReason.PARKED,
            FakePage("File was deleted. alpha-unique") to UnresolvedReason.NOT_FOUND,
            FakePage("<html>alpha-unique</html>", status = 404) to UnresolvedReason.NOT_FOUND,
            FakePage("") to UnresolvedReason.EMPTY_PAGE,
        ).forEach { (page, reason) ->
            val (resolution, _) = resolve(unknown, mapOf(unknown to page))
            val unresolved = assertIs<Resolution.Unresolved>(resolution)
            assertEquals(reason, unresolved.reason, page.body)
        }
    }

    @Test fun notFoundPagesStillNameTheSuspectedServer() {
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("<p>alpha-unique</p>", status = 404)))
        assertEquals(listOf("Alpha"), (resolution as Resolution.Unresolved).suspects)
    }

    @Test fun fingerprintDecidesAndIsLearnableOnTheInputHost() {
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("<p>beta-unique</p>")))
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.FINGERPRINT, resolved.stage)
        assertEquals("Beta", resolved.primary.factory.serverName)
        assertEquals("unknown-mirror.xyz", resolved.learnableHost)
        assertTrue(resolved.page != null)
    }

    @Test fun fingerprintAfterAnHttpRedirectToAnotherUnknownHostIsNotLearned() {
        val moved = "https://moved-mirror.xyz/e/abc123"
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("<p>beta-unique</p>", finalUrl = moved)))
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(moved, resolved.primary.url)
        assertNull(resolved.learnableHost)
    }

    @Test fun familyPagesProduceOrderedCandidatesCappedByConfig() {
        val x3 = fakeFactory("X3", listOf("x3.com"), family = TestFamily)
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("family-a family-b")),
            ResolutionConfig(fallbackEnabled = true, maxCandidateAttempts = 2), factories + x3)
        val resolved = assertIs<Resolution.Resolved>(resolution)
        assertEquals(ResolutionStage.FAMILY_PROBE, resolved.stage)
        assertEquals(listOf("X1", "X2"), resolved.candidates.map { it.factory.serverName })
    }

    @Test fun ambiguousPagesNameTheContenders() {
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("alpha-unique beta-unique")))
        val unresolved = assertIs<Resolution.Unresolved>(resolution)
        assertEquals(UnresolvedReason.AMBIGUOUS, unresolved.reason)
        assertEquals(setOf("Alpha", "Beta"), unresolved.suspects.toSet())
    }

    @Test fun pagesWithoutEvidenceAreUnresolved() {
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage("<html><title>Loading...</title></html>")))
        assertEquals(UnresolvedReason.NO_EVIDENCE, (resolution as Resolution.Unresolved).reason)
    }

    @Test fun pageBodiesAreCappedBeforeInspection() {
        val late = "x".repeat(5_000) + "alpha-unique"
        val (resolution, _) = resolve(unknown, mapOf(unknown to FakePage(late)), ResolutionConfig(fallbackEnabled = true, maxBodyChars = 2_048))
        assertEquals(UnresolvedReason.NO_EVIDENCE, (resolution as Resolution.Unresolved).reason)
    }

    // ---- Learned hosts ----

    @Test fun learnedHostsRouteStaticallyWhenThePathFits() {
        val store = InMemoryLearnedHostStore().apply { remember("unknown-mirror.xyz", "Beta") }
        val resolver = ServerResolver(factories, ResolutionConfig(learnedHosts = store))
        val resolved = assertIs<Resolution.Resolved>(resolver.resolveStatic(unknown))
        assertEquals(ResolutionStage.LEARNED_HOST, resolved.stage)
        assertEquals("Beta", resolved.primary.factory.serverName)
        // The learned server's path shape must still match.
        assertIs<Resolution.Unresolved>(resolver.resolveStatic("https://unknown-mirror.xyz/watch/abc123"))
        // A learned name that is no longer installed is ignored.
        store.remember("gone.xyz", "Removed")
        assertIs<Resolution.Unresolved>(resolver.resolveStatic("https://gone.xyz/e/abc123"))
    }

    @Test fun inMemoryStoreIsBoundedAndCaseInsensitive() {
        val store = InMemoryLearnedHostStore(maxEntries = 2)
        store.remember("A.com", "One")
        store.remember("b.com", "Two")
        store.remember("c.com", "Three")
        assertNull(store.serverFor("a.com"))
        assertEquals("Two", store.serverFor("B.COM"))
        store.remember("b.com", "Two")
        store.remember("d.com", "Four")
        assertEquals(listOf("b.com", "d.com"), store.snapshot().keys.toList())
        store.forget("B.com")
        assertNull(store.serverFor("b.com"))
        assertFailsWith<IllegalArgumentException> { InMemoryLearnedHostStore(0) }
    }

    @Test fun fallbackIsOnByDefault() {
        assertTrue(ResolutionConfig().fallbackEnabled)
        val (resolution, client) = resolve(unknown, mapOf(unknown to FakePage("<p>beta-unique</p>")), config = ResolutionConfig())
        assertEquals("Beta", (resolution as Resolution.Resolved).primary.factory.serverName)
        assertEquals(1, client.requests.size)
    }

    @Test fun configValidatesBounds() {
        assertFailsWith<IllegalArgumentException> { ResolutionConfig(maxNavigationHops = 0) }
        assertFailsWith<IllegalArgumentException> { ResolutionConfig(maxCandidateAttempts = 11) }
        assertFailsWith<IllegalArgumentException> { ResolutionConfig(maxBodyChars = 10) }
    }
}
