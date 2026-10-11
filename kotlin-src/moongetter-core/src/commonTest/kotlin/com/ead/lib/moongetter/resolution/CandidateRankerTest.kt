package com.ead.lib.moongetter.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The ranking rules for pages that look alike. Every case is checked with the factories in both
 * orders where order must not matter.
 */
class CandidateRankerTest {

    private val alpha = fakeFactory("Alpha", listOf("alpha.com"), listOf(
        unique("alpha.unique", "alpha-unique"), supporting("alpha.support", "alpha-support"), supporting("alpha.support2", "alpha-two"),
    ))
    private val beta = fakeFactory("Beta", listOf("beta.com"), listOf(unique("beta.unique", "beta-unique"), supporting("beta.support", "beta-support")))
    private val gamma = fakeFactory("Gamma", listOf("gamma.com"), listOf(supporting("gamma.support", "gamma-support"), supporting("gamma.support2", "gamma-two")))
    private val x1 = fakeFactory("X1", listOf("x1.com"), listOf(unique("x1.unique", "x1-unique"), supporting("x1.support", "x1-support")), TestFamily)
    private val x2 = fakeFactory("X2", listOf("x2.com"), listOf(supporting("x2.support", "x2-support")), TestFamily)
    private val x3 = fakeFactory("X3", listOf("x3.com"), family = TestFamily)
    private val all = listOf(alpha, beta, gamma, x1, x2, x3)
    private val ranker = CandidateRanker()

    private fun rankBothOrders(body: String, url: String = "https://unknown.xyz/e/abc123"): Pair<RankingDecision, RankingDecision> =
        ranker.rank(page(body, url), all) to ranker.rank(page(body, url), all.reversed())

    @Test fun oneUniqueSignalDecides() {
        val (forward, backward) = rankBothOrders("<p>alpha-unique</p>")
        listOf(forward, backward).forEach {
            assertIs<RankingDecision.Decisive>(it)
            assertEquals("Alpha", it.winner.serverName)
            assertEquals(listOf("alpha.unique"), it.winner.uniqueHits)
        }
    }

    @Test fun supportingSignalsAddConfidenceButNeverDecideAlone() {
        assertIs<RankingDecision.Insufficient>(ranker.rank(page("alpha-support alpha-two"), all))
        assertIs<RankingDecision.Insufficient>(ranker.rank(page("gamma-support gamma-two"), all))
        val decisive = assertIs<RankingDecision.Decisive>(ranker.rank(page("alpha-unique alpha-support alpha-two"), all))
        assertEquals(50 + 15 + 15 + PathRule.GENERIC_PRIOR, decisive.winner.score)
    }

    @Test fun uniqueSignalsFromTwoServersAreAmbiguousWhateverTheScores() {
        // Beta has one unique marker, Alpha has a unique marker plus all supporting markers: still ambiguous.
        val (forward, backward) = rankBothOrders("alpha-unique alpha-support alpha-two beta-unique")
        listOf(forward, backward).forEach {
            val ambiguous = assertIs<RankingDecision.Ambiguous>(it)
            assertEquals(setOf("Alpha", "Beta"), ambiguous.contenders.map { c -> c.serverName }.toSet())
        }
    }

    @Test fun uniqueWinnerNeedsAMarginOverTheRunnerUp() {
        // Alpha: unique (50) + path; Gamma: two supporting (30) + path. Lead of 20 is below the margin of 30.
        val tight = ranker.rank(page("alpha-unique gamma-support gamma-two"), all)
        val ambiguous = assertIs<RankingDecision.Ambiguous>(tight)
        assertEquals(listOf("Alpha", "Gamma"), ambiguous.contenders.map { it.serverName })
        // One supporting marker from Gamma leaves a sufficient lead.
        assertIs<RankingDecision.Decisive>(ranker.rank(page("alpha-unique gamma-support"), all))
    }

    @Test fun pathShapeAloneIsNotEvidence() {
        val scores = ranker.score(page("<html>nothing</html>"), all)
        assertTrue(scores.all { it.score == 0 }, scores.toString())
        assertIs<RankingDecision.Insufficient>(ranker.decide(scores))
    }

    @Test fun pathShapeAddsItsPriorNextToRealEvidence() {
        val rpmLike = fakeFactory("Fragment", listOf("frag.com"), listOf(unique("frag.unique", "frag-unique")),
            paths = listOf(PathRule("""/?#([a-z0-9]+)""", prior = PathRule.DISTINCTIVE_PRIOR)))
        val onShape = ranker.score(page("frag-unique", "https://unknown.xyz/#abc"), listOf(rpmLike)).single()
        val offShape = ranker.score(page("frag-unique", "https://unknown.xyz/watch/abc"), listOf(rpmLike)).single()
        assertEquals(50 + PathRule.DISTINCTIVE_PRIOR, onShape.score)
        assertEquals(50, offShape.score)
    }

    @Test fun familyMarkersWithoutMemberEvidenceProbeTheFamily() {
        val decision = assertIs<RankingDecision.Family>(ranker.rank(page("family-a family-b"), all))
        assertEquals(TestFamily, decision.family)
        // X1 has branding of its own (a unique marker) that this page lacks, so it is tried last.
        assertEquals(listOf("X2", "X3", "X1"), decision.members.map { it.serverName })
        // Otherwise equal evidence keeps engine order, so the caller controls the probing order.
        val reversed = assertIs<RankingDecision.Family>(ranker.rank(page("family-a family-b"), all.reversed()))
        assertEquals(listOf("X3", "X2", "X1"), reversed.members.map { it.serverName })
    }

    @Test fun supportingEvidenceReordersFamilyProbing() {
        val decision = assertIs<RankingDecision.Family>(ranker.rank(page("family-a family-b x2-support"), all))
        assertEquals("X2", decision.members.first().serverName)
        assertEquals(listOf("X2", "X3", "X1"), decision.members.map { it.serverName })
    }

    @Test fun familyNeedsTheMinimumNumberOfMarkers() {
        assertIs<RankingDecision.Insufficient>(ranker.rank(page("family-a"), all))
        val all3 = assertIs<RankingDecision.Family>(ranker.rank(page("family-a family-b family-c"), all))
        assertEquals(3, all3.members.first().familyHits.size)
    }

    @Test fun memberUniqueSignalBeatsItsSiblings() {
        val (forward, backward) = rankBothOrders("family-a family-b x1-unique")
        listOf(forward, backward).forEach {
            assertEquals("X1", assertIs<RankingDecision.Decisive>(it).winner.serverName)
        }
    }

    @Test fun twoMembersWithUniqueSignalsAreAmbiguous() {
        val x2Unique = fakeFactory("X2", listOf("x2.com"), listOf(unique("x2.unique", "x2-unique")), TestFamily)
        val decision = ranker.rank(page("family-a family-b x1-unique x2-unique"), listOf(x1, x2Unique, x3))
        assertEquals(setOf("X1", "X2"), assertIs<RankingDecision.Ambiguous>(decision).contenders.map { it.serverName }.toSet())
    }

    @Test fun outsiderWithComparableEvidenceBlocksFamilyProbing() {
        // Family members: 2 family markers (20) + path (5) = 25. Gamma: two supporting (30) + path.
        val decision = ranker.rank(page("family-a family-b gamma-support gamma-two"), all)
        assertEquals(setOf("Gamma"), assertIs<RankingDecision.Ambiguous>(decision).contenders.map { it.serverName }.toSet() - setOf("X1", "X2", "X3"))
        // A weaker outsider does not block it.
        assertIs<RankingDecision.Family>(ranker.rank(page("family-a family-b family-c gamma-support"), all))
    }

    @Test fun uniqueOutsiderAgainstAFamilyPageDecidesOnlyWithMargin() {
        // Alpha unique (50+5) vs family members (2×10+5=25): lead 30 meets the margin.
        assertEquals("Alpha", assertIs<RankingDecision.Decisive>(ranker.rank(page("alpha-unique family-a family-b"), all)).winner.serverName)
        // With all three family markers the members reach 35 and the lead drops to 20.
        assertIs<RankingDecision.Ambiguous>(ranker.rank(page("alpha-unique family-a family-b family-c"), all))
    }

    @Test fun twoFamiliesAreAmbiguous() {
        val other = ServerFamily("other", listOf(familyMarker("o.a", "other-a"), familyMarker("o.b", "other-b")))
        val y1 = fakeFactory("Y1", listOf("y1.com"), family = other)
        val decision = ranker.rank(page("family-a family-b other-a other-b"), listOf(x2, x3, y1))
        assertIs<RankingDecision.Ambiguous>(decision)
    }

    @Test fun factoriesWithoutDescriptorAreNeverScored() {
        val legacy = FakeFactory("Legacy", descriptor = null, legacyPattern = "alpha-unique")
        val scores = ranker.score(page("alpha-unique"), listOf(legacy, alpha))
        assertEquals(listOf("Alpha"), scores.map { it.serverName })
    }

    @Test fun policyIsConfigurableButGuardsAgainstSupportingOnlyDecisions() {
        val strict = CandidateRanker(ScoringPolicy(decisiveMargin = 60))
        assertIs<RankingDecision.Ambiguous>(strict.rank(page("alpha-unique gamma-support"), all))
        assertFailsWith<IllegalArgumentException> { ScoringPolicy(supportingWeight = 30, decisiveScore = 50) }
        assertFailsWith<IllegalArgumentException> { ScoringPolicy(uniqueWeight = 0) }
    }
}
