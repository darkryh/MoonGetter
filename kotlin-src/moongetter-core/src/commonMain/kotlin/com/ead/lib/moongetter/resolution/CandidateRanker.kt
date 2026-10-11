package com.ead.lib.moongetter.resolution

import com.ead.lib.moongetter.models.Server

/**
 * Weights and thresholds used to turn signal hits into a decision.
 *
 * The defaults encode the rule "a wrong server is worse than no server":
 * - only a [SignalKind.UNIQUE] signal can make a single server win;
 * - if two different servers both show unique signals the page is ambiguous, whatever the scores;
 * - the winner must lead the runner-up by [decisiveMargin].
 */
data class ScoringPolicy(
    val uniqueWeight: Int = 50,
    val supportingWeight: Int = 15,
    val familyWeight: Int = 10,
    /** Minimum score for a single server to win. */
    val decisiveScore: Int = 50,
    /** Minimum lead over the runner-up for a single server to win. */
    val decisiveMargin: Int = 30,
) {
    init {
        require(uniqueWeight > 0 && supportingWeight >= 0 && familyWeight >= 0) { "Weights must be positive" }
        require(decisiveScore > 0 && decisiveMargin > 0) { "Thresholds must be positive" }
        require(supportingWeight * 2 < decisiveScore) { "Two supporting signals must not decide on their own" }
    }
}

/** The evidence one server collected for one page. */
data class CandidateScore(
    val factory: Server.Factory,
    val score: Int,
    val uniqueHits: List<String>,
    val supportingHits: List<String>,
    val familyHits: List<String>,
    val pathPrior: Int,
) {
    val serverName: String get() = factory.serverName

    override fun toString(): String =
        "$serverName=$score(unique=$uniqueHits, supporting=$supportingHits, family=$familyHits, path=$pathPrior)"
}

/** The outcome of ranking a page. */
sealed class RankingDecision {
    abstract val scores: List<CandidateScore>

    /** Exactly one server is identified by unique evidence. */
    data class Decisive(val winner: CandidateScore, override val scores: List<CandidateScore>) : RankingDecision()

    /** The page belongs to [family] but no member is identified; try the members in [members] order. */
    data class Family(
        val family: ServerFamily,
        val members: List<CandidateScore>,
        override val scores: List<CandidateScore>,
    ) : RankingDecision()

    /** Evidence points at more than one server and none can be preferred safely. */
    data class Ambiguous(val contenders: List<CandidateScore>, override val scores: List<CandidateScore>) : RankingDecision()

    /** No server has enough evidence. */
    data class Insufficient(override val scores: List<CandidateScore>) : RankingDecision()
}

/**
 * Scores every fingerprinted server against a page and applies [ScoringPolicy].
 */
class CandidateRanker(private val policy: ScoringPolicy = ScoringPolicy()) {

    /** Scores each factory that has a [ServerDescriptor]; factories without one cannot be identified by HTML. */
    fun score(page: PageSnapshot, factories: List<Server.Factory>): List<CandidateScore> {
        val pageUrl = EmbedUrl.parse(page.finalUrl)
        val familyHits = mutableMapOf<ServerFamily, List<String>>()
        return factories.mapNotNull { factory ->
            val descriptor = factory.descriptor ?: return@mapNotNull null
            val signals = descriptor.fingerprint?.signals.orEmpty()
            val unique = signals.filter { it.kind == SignalKind.UNIQUE && it.matches(page) }.map { it.id }
            val supporting = signals.filter { it.kind == SignalKind.SUPPORTING && it.matches(page) }.map { it.id }
            val family = descriptor.family?.let { family ->
                familyHits.getOrPut(family) { family.signals.filter { it.matches(page) }.map { it.id } }
                    .takeIf { it.size >= family.minimumHits }
            }.orEmpty()
            val prior = pageUrl?.let { descriptor.matchPath(it)?.rule?.prior } ?: 0
            val evidence = unique.size * policy.uniqueWeight + supporting.size * policy.supportingWeight +
                family.size * policy.familyWeight
            // A path shape alone is not evidence of anything: it only counts next to real page evidence.
            val score = if (evidence == 0) 0 else evidence + prior
            CandidateScore(factory, score, unique, supporting, family, prior)
        }.sortedByDescending { it.score }
    }

    /** Ranks [page] against [factories] in engine order (used to break ties deterministically). */
    fun rank(page: PageSnapshot, factories: List<Server.Factory>): RankingDecision = decide(score(page, factories))

    /** Applies the policy to precomputed [scores] (sorted or not). */
    fun decide(scores: List<CandidateScore>): RankingDecision {
        val ranked = scores.filter { it.score > 0 }.sortedByDescending { it.score }
        if (ranked.isEmpty()) return RankingDecision.Insufficient(scores)

        val uniqueHolders = ranked.filter { it.uniqueHits.isNotEmpty() }
        if (uniqueHolders.size > 1) return RankingDecision.Ambiguous(uniqueHolders, scores)
        if (uniqueHolders.size == 1) {
            val winner = uniqueHolders.single()
            val runnerUp = ranked.firstOrNull { it !== winner }?.score ?: 0
            return if (winner.score >= policy.decisiveScore && winner.score - runnerUp >= policy.decisiveMargin) {
                RankingDecision.Decisive(winner, scores)
            } else {
                RankingDecision.Ambiguous(ranked.take(2), scores)
            }
        }

        val families = ranked.mapNotNull { score -> score.factory.descriptor?.family?.takeIf { score.familyHits.isNotEmpty() } }.toSet()
        if (families.size == 1) {
            val family = families.single()
            // Ties go to members without branding of their own: a member whose unique markers exist but are
            // missing from the page is less likely than one that never shows any. Then engine order.
            val members = ranked.filter { it.factory.descriptor?.family == family }
                .sortedWith(compareByDescending<CandidateScore> { it.score }.thenBy { it.hasOwnBranding() })
            val familyBest = members.first().score
            val outsider = ranked.firstOrNull { it.factory.descriptor?.family != family }
            // An unrelated server with comparable evidence makes the family call unsafe.
            if (outsider != null && outsider.score >= familyBest) return RankingDecision.Ambiguous(listOf(members.first(), outsider), scores)
            return RankingDecision.Family(family, members, scores)
        }
        if (families.size > 1) return RankingDecision.Ambiguous(ranked, scores)
        return RankingDecision.Insufficient(scores)
    }

    private fun CandidateScore.hasOwnBranding(): Boolean =
        factory.descriptor?.fingerprint?.signals.orEmpty().any { it.kind == SignalKind.UNIQUE }
}
