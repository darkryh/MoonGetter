package com.ead.lib.moongetter.server.bundle

import com.ead.lib.moongetter.resolution.CandidateRanker
import com.ead.lib.moongetter.resolution.PageClassifier
import com.ead.lib.moongetter.resolution.PageKind
import com.ead.lib.moongetter.resolution.PageSnapshot
import com.ead.lib.moongetter.resolution.Resolution
import com.ead.lib.moongetter.resolution.ResolutionConfig
import com.ead.lib.moongetter.resolution.ServerFamily
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.Fixture
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.factories
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.fixtures
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real pages captured from every researched service, resolved through the full fallback against the
 * real server bundle. The governing rule is that no page may ever resolve to the wrong server.
 */
class FixtureResolutionTest {

    private val config = ResolutionConfig(fallbackEnabled = true, learnedHosts = null)

    private fun resolve(fixture: Fixture): Pair<Resolution, PageClient> = runBlocking {
        val client = PageClient(mapOf(fixture.url to (fixture.status to fixture.html)))
        BundleTestSupport.resolver(config).resolve(fixture.url, client, mapOf("User-Agent" to "test")) to client
    }

    private fun describe(resolution: Resolution): String = when (resolution) {
        is Resolution.Resolved -> "${resolution.stage} -> ${resolution.candidates.joinToString { it.factory.serverName }}"
        is Resolution.Unresolved -> "unresolved ${resolution.reason}" + resolution.suspects.takeIf { it.isNotEmpty() }?.let { " suspects=$it" }.orEmpty()
    }

    @Test fun everyFixtureResolvesAsLabelled() {
        val report = StringBuilder("\nfixture                    truth        expected                              actual\n")
        val failures = mutableListOf<String>()
        fixtures.forEach { fixture ->
            val (resolution, _) = resolve(fixture)
            val expected = fixture.expect
            val ok = when (resolution) {
                is Resolution.Resolved -> expected.outcome == "resolved" &&
                    resolution.stage.name == expected.stage &&
                    resolution.primary.factory.serverName == expected.server &&
                    (expected.url == null || resolution.primary.url == expected.url) &&
                    (expected.candidates == null || resolution.candidates.map { it.factory.serverName } == expected.candidates)
                is Resolution.Unresolved -> expected.outcome == "unresolved" &&
                    resolution.reason.name == expected.reason &&
                    (expected.suspects == null || resolution.suspects == expected.suspects)
            }
            val expectedText = if (expected.outcome == "resolved") "${expected.stage} -> ${expected.server}" else "unresolved ${expected.reason}"
            report.append("%-26s %-12s %-37s %s%s\n".format(fixture.file, fixture.truth ?: "-", expectedText, describe(resolution), if (ok) "" else "   <-- MISMATCH"))
            if (!ok) failures += "${fixture.file}: expected $expectedText, got ${describe(resolution)}"
        }
        println(report)
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test fun noFixtureEverResolvesToTheWrongServer() {
        fixtures.forEach { fixture ->
            val (resolution, _) = resolve(fixture)
            if (resolution is Resolution.Resolved) {
                assertEquals(fixture.truth, resolution.primary.factory.serverName, "${fixture.file} resolved to the wrong server")
                if (resolution.stage == com.ead.lib.moongetter.resolution.ResolutionStage.FAMILY_PROBE) {
                    assertTrue(resolution.candidates.any { it.factory.serverName == fixture.truth }, fixture.file)
                }
            }
        }
    }

    @Test fun fallbackFetchesEachFixtureOnceAndFollowsOnlyLiteralNavigation() {
        fixtures.forEach { fixture ->
            val (_, client) = resolve(fixture)
            assertEquals(fixture.url, client.requests.first(), fixture.file)
            assertTrue(client.requests.size <= config.maxNavigationHops, "${fixture.file}: ${client.requests}")
        }
    }

    @Test fun uniqueSignalsAreUniqueAcrossTheCorpus() {
        // A signal declared UNIQUE for one server must never fire on a page that belongs to another server.
        val pages = fixtures.map { it to PageSnapshot(it.url, it.url, it.status, emptyMap(), it.html) }
        val failures = factories.flatMap { factory ->
            factory.descriptor?.fingerprint?.signals.orEmpty().filter { it.kind == SignalKind.UNIQUE }.flatMap { signal ->
                pages.filter { (fixture, page) -> fixture.truth != factory.serverName && signal.matches(page) }
                    .map { (fixture, _) -> "${signal.id} (${factory.serverName}) fires on ${fixture.file} (${fixture.truth})" }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test fun everyPlayerFixtureCarriesItsOwnUniqueSignal() {
        val brandedPlayers = fixtures.filter { it.role == "player" && it.truth != "Streamwish" }
        brandedPlayers.forEach { fixture ->
            val page = PageSnapshot(fixture.url, fixture.url, fixture.status, emptyMap(), fixture.html)
            val signals = BundleTestSupport.factory(fixture.truth!!).descriptor!!.fingerprint!!.signals
            assertTrue(signals.any { it.kind == SignalKind.UNIQUE && it.matches(page) }, "${fixture.file} has none of ${fixture.truth}'s unique signals")
        }
    }

    @Test fun xFileSharingPlayersShowTheFamilyMarkers() {
        listOf("streamwish-1.html", "vidhide-1.html", "lulustream-2.html").forEach { file ->
            val fixture = fixtures.single { it.file == file }
            val page = PageSnapshot(fixture.url, fixture.url, fixture.status, emptyMap(), fixture.html)
            val hits = ServerFamily.XFileSharing.signals.filter { it.matches(page) }
            assertTrue(hits.size >= ServerFamily.XFileSharing.minimumHits, "$file family hits: ${hits.map { it.id }}")
        }
        // No other service's page looks like XFileSharing.
        fixtures.filter { it.truth !in setOf("Streamwish", "Vihide", "Lulustream") }.forEach { fixture ->
            val page = PageSnapshot(fixture.url, fixture.url, fixture.status, emptyMap(), fixture.html)
            val hits = ServerFamily.XFileSharing.signals.filter { it.matches(page) }
            assertTrue(hits.size < ServerFamily.XFileSharing.minimumHits, "${fixture.file} looks like XFileSharing: ${hits.map { it.id }}")
        }
    }

    @Test fun pageStateClassificationMatchesFixtureRoles() {
        fixtures.forEach { fixture ->
            val kind = PageClassifier.classify(PageSnapshot(fixture.url, fixture.url, fixture.status, emptyMap(), fixture.html))
            val expected = when {
                fixture.role.startsWith("cloudflare") -> PageKind.CHALLENGE
                fixture.role.startsWith("parked") || fixture.role.startsWith("for sale") ||
                    fixture.role.startsWith("coming soon") || fixture.role.startsWith("marketplace") -> PageKind.PARKED
                fixture.role == "deleted file" -> PageKind.NOT_FOUND
                else -> PageKind.CONTENT
            }
            assertEquals(expected, kind, fixture.file)
        }
    }

    @Test fun scoresAreReportedForEveryContentFixture() {
        val ranker = CandidateRanker()
        val report = StringBuilder("\nscores per content fixture (non-zero only)\n")
        fixtures.filter { it.expect.outcome == "resolved" || it.expect.reason == "NO_EVIDENCE" }.forEach { fixture ->
            val page = PageSnapshot(fixture.url, fixture.url, fixture.status, emptyMap(), fixture.html)
            val scores = ranker.score(page, factories).filter { it.score > 0 }
            report.append("%-20s %s\n".format(fixture.file, scores.joinToString { "${it.serverName}=${it.score}" }))
        }
        println(report)
    }
}
