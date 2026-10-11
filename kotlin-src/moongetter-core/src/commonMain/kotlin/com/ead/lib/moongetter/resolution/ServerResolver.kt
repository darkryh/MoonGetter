package com.ead.lib.moongetter.resolution

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.models.Server
import kotlin.coroutines.cancellation.CancellationException

/**
 * Decides which server handles a URL, from cheapest to most expensive evidence:
 *
 * 1. **Known host**: host and path listed by exactly one server (no network).
 * 2. **Learned host**: host confirmed by an earlier fallback (no network).
 * 3. **Redirect**: fetch once; the final URL after HTTP redirects, or a literal script/meta navigation,
 *    is a known host.
 * 4. **Embedded frame**: the page wraps a known host in an iframe.
 * 5. **Fingerprint / family**: classify the page, then score server fingerprints.
 *
 * Steps 3–5 only run when [ResolutionConfig.fallbackEnabled] is true. This class never runs extractors;
 * the caller confirms a fallback answer by extracting with its candidates.
 *
 * @param factories Servers in engine order. Order breaks ties between family members.
 */
class ServerResolver(
    factories: List<Server.Factory>,
    private val config: ResolutionConfig = ResolutionConfig(),
) {
    private val factories = factories.toList()
    private val ranker = CandidateRanker(config.scoring)

    /** Steps 1–2 only. Never touches the network. */
    fun resolveStatic(url: String): Resolution {
        val parsed = EmbedUrl.parse(url)
        val descriptorMatches = parsed?.let { target ->
            factories.filter { it.descriptor?.match(target) != null }
        }.orEmpty()
        if (descriptorMatches.size == 1) return resolved(url, ResolutionStage.KNOWN_HOST, descriptorMatches.single(), url, listOf("host:${parsed?.host}"))
        if (descriptorMatches.size > 1) return conflict(url, descriptorMatches)

        // Servers without a descriptor keep their historical pattern semantics.
        val legacyMatches = factories.filter { factory ->
            factory.descriptor == null && runCatching { Regex(factory.pattern).containsMatchIn(url) }.getOrDefault(false)
        }
        if (legacyMatches.size == 1) return resolved(url, ResolutionStage.KNOWN_HOST, legacyMatches.single(), url, listOf("pattern"))
        if (legacyMatches.size > 1) return conflict(url, legacyMatches)

        if (parsed == null) return Resolution.Unresolved(url, UnresolvedReason.INVALID_URL, listOf("unparseable URL"))
        learned(parsed)?.let { return it }
        return Resolution.Unresolved(url, UnresolvedReason.NO_KNOWN_HOST, listOf("host:${parsed.host}"))
    }

    /** All steps. Fetches with [client] and [headers] only when the fallback is enabled and needed. */
    suspend fun resolve(url: String, client: MoonClient, headers: Map<String, String>): Resolution {
        val static = resolveStatic(url)
        if (static is Resolution.Resolved || !config.fallbackEnabled) return static
        static as Resolution.Unresolved
        if (static.reason != UnresolvedReason.NO_KNOWN_HOST) return static
        return probe(url, EmbedUrl.parse(url) ?: return static, client, headers)
    }

    private suspend fun probe(input: String, start: EmbedUrl, client: MoonClient, headers: Map<String, String>): Resolution {
        val evidence = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        var target = start
        var lastScores: List<CandidateScore> = emptyList()

        repeat(config.maxNavigationHops) { hop ->
            UrlSafety.rejectionReason(target)?.let { reason ->
                return Resolution.Unresolved(input, UnresolvedReason.UNSAFE_URL, evidence + "unsafe:${target.host}:$reason")
            }
            if (!visited.add(target.toString())) return unresolvedFromScores(input, UnresolvedReason.NO_EVIDENCE, evidence + "navigation-loop", lastScores)

            val page = try {
                fetch(target, client, headers)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                return Resolution.Unresolved(input, UnresolvedReason.FETCH_FAILED, evidence + "fetch:${target.host}:${error::class.simpleName}")
            }
            evidence += "fetched:${page.statusCode}:${EmbedUrl.parse(page.finalUrl)?.host ?: page.finalUrl}"

            // 3a. The client followed HTTP redirects: is the final URL a known host?
            val final = EmbedUrl.parse(page.finalUrl)
                ?: return Resolution.Unresolved(input, UnresolvedReason.FETCH_FAILED, evidence + "unparseable-final-url")
            UrlSafety.rejectionReason(final)?.let { reason ->
                return Resolution.Unresolved(input, UnresolvedReason.UNSAFE_URL, evidence + "unsafe-redirect:${final.host}:$reason")
            }
            if (final.toString() != target.toString()) {
                knownFactory(final)?.let { factory ->
                    // Only an HTTP redirect from the very first URL makes the input host safe to learn:
                    // the server will be given URLs on that host and its client follows HTTP redirects too.
                    val learnable = if (hop == 0) start.host else null
                    return resolved(input, ResolutionStage.REDIRECT, factory, final.toString(), evidence + "http-redirect:${final.host}", page, learnable)
                }
            }

            when (PageClassifier.classify(page)) {
                PageKind.CHALLENGE -> return unresolvedWithSuspects(input, UnresolvedReason.CHALLENGE, evidence + "page:challenge", page)
                PageKind.PARKED -> return Resolution.Unresolved(input, UnresolvedReason.PARKED, evidence + "page:parked")
                PageKind.NOT_FOUND -> return unresolvedWithSuspects(input, UnresolvedReason.NOT_FOUND, evidence + "page:not-found", page)
                PageKind.EMPTY -> return Resolution.Unresolved(input, UnresolvedReason.EMPTY_PAGE, evidence + "page:empty")
                PageKind.CONTENT -> Unit
            }

            // 3b/4. Literal navigations and frames pointing at known hosts.
            val navigations = PageNavigation.find(page)
            for (navigation in navigations) {
                val factory = knownFactory(navigation.target) ?: continue
                val stage = if (navigation.kind == NavigationKind.FRAME) ResolutionStage.EMBEDDED_FRAME else ResolutionStage.REDIRECT
                // Known hosts are listed public services; the server itself will fetch the target.
                return resolved(input, stage, factory, navigation.target.toString(),
                    evidence + "${navigation.kind.name.lowercase()}:${navigation.target.host}", page, learnableHost = null)
            }

            // 5. Fingerprints.
            when (val decision = ranker.rank(page, factories)) {
                is RankingDecision.Decisive -> return Resolution.Resolved(
                    url = input,
                    stage = ResolutionStage.FINGERPRINT,
                    candidates = listOf(ResolutionCandidate(decision.winner.factory, final.toString())),
                    evidence = evidence + "fingerprint:${decision.winner}",
                    page = page,
                    learnableHost = start.host.takeIf { hop == 0 && final.host == start.host },
                )
                is RankingDecision.Family -> return Resolution.Resolved(
                    url = input,
                    stage = ResolutionStage.FAMILY_PROBE,
                    candidates = decision.members.take(config.maxCandidateAttempts).map { ResolutionCandidate(it.factory, final.toString()) },
                    evidence = evidence + "family:${decision.family.id}:${decision.members}",
                    page = page,
                    learnableHost = start.host.takeIf { hop == 0 && final.host == start.host },
                )
                is RankingDecision.Ambiguous -> return Resolution.Unresolved(
                    input, UnresolvedReason.AMBIGUOUS, evidence + "ambiguous:${decision.contenders}",
                    decision.contenders.map { it.serverName },
                )
                is RankingDecision.Insufficient -> lastScores = decision.scores
            }

            // Nothing identified this page: follow its first unvisited script or meta navigation, if any.
            target = navigations.firstOrNull { it.kind != NavigationKind.FRAME && it.target.toString() !in visited }?.target
                ?: return unresolvedFromScores(input, UnresolvedReason.NO_EVIDENCE, evidence, lastScores)
            evidence += "follow:${target.host}"
        }
        return unresolvedFromScores(input, UnresolvedReason.NO_EVIDENCE, evidence + "hop-limit", lastScores)
    }

    private suspend fun fetch(target: EmbedUrl, client: MoonClient, headers: Map<String, String>): PageSnapshot {
        val response = client.request<String>(Request(method = HttpMethod.GET, url = target.toString(), headers = headers))
        return PageSnapshot(
            requestedUrl = target.toString(),
            finalUrl = response.url.toString.ifEmpty { target.toString() },
            statusCode = response.statusCode,
            headers = response.headers,
            body = response.body.asString(),
            maxBodyChars = config.maxBodyChars,
        )
    }

    /** The single descriptor-backed server listing [url]'s host and path, or `null` (none or several). */
    private fun knownFactory(url: EmbedUrl): Server.Factory? =
        factories.filter { it.descriptor?.match(url) != null }.singleOrNull()

    private fun learned(url: EmbedUrl): Resolution? {
        val store = config.learnedHosts ?: return null
        val name = store.serverFor(url.host) ?: return null
        val factory = factories.firstOrNull { it.serverName == name } ?: return null
        val descriptor = factory.descriptor ?: return null
        descriptor.matchPath(url) ?: return null
        return resolved(url.raw, ResolutionStage.LEARNED_HOST, factory, url.raw, listOf("learned:${url.host}"))
    }

    private fun resolved(
        input: String,
        stage: ResolutionStage,
        factory: Server.Factory,
        url: String,
        evidence: List<String>,
        page: PageSnapshot? = null,
        learnableHost: String? = null,
    ) = Resolution.Resolved(input, stage, listOf(ResolutionCandidate(factory, url)), evidence, page, learnableHost)

    private fun conflict(url: String, factories: List<Server.Factory>) = Resolution.Unresolved(
        url, UnresolvedReason.CONFLICTING_SERVERS, listOf("claimed-by:${factories.joinToString { it.serverName }}"),
        factories.map { it.serverName },
    )

    private fun unresolvedWithSuspects(input: String, reason: UnresolvedReason, evidence: List<String>, page: PageSnapshot) =
        unresolvedFromScores(input, reason, evidence, ranker.score(page, factories))

    private fun unresolvedFromScores(input: String, reason: UnresolvedReason, evidence: List<String>, scores: List<CandidateScore>) =
        Resolution.Unresolved(input, reason, evidence, scores.filter { it.score > 0 }.map { it.serverName })
}
