@file:OptIn(ExperimentalAtomicApi::class)

package com.ead.lib.moongetter.resolution

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Controls how URLs that no server recognizes are handled.
 *
 * The network fallback is **on by default**. A URL whose host no server lists is fetched once
 * (HTTPS only, public hostnames only), its redirects and literal navigations are checked against
 * known hosts, and finally its HTML is fingerprinted. A server chosen this way is only accepted after
 * its extractor returns videos. URLs on known hosts never trigger it. Server deployments that resolve
 * untrusted URLs should also restrict outbound network access (see [UrlSafety]), or disable it.
 *
 * @property fallbackEnabled Whether unrecognized URLs may be fetched to identify their server.
 * @property maxNavigationHops Maximum number of pages fetched for one URL, including the first.
 * @property maxCandidateAttempts Maximum number of extractors tried when a page matches a server family.
 * @property maxBodyChars Size cap applied to fetched HTML before it is inspected.
 * @property reuseFetchedPage Whether the identified server receives the already-fetched page for its
 * first request to the same URL instead of fetching it again.
 * @property learnedHosts Remembers hosts identified by the fallback so the next URL on them is routed
 * without a network request. `null` disables learning.
 * @property listener Receives resolution events, for example to report new domains upstream.
 * @property scoring Weights and thresholds for fingerprint decisions.
 */
data class ResolutionConfig(
    val fallbackEnabled: Boolean = true,
    val maxNavigationHops: Int = 3,
    val maxCandidateAttempts: Int = 3,
    val maxBodyChars: Int = PageSnapshot.DEFAULT_MAX_BODY_CHARS,
    val reuseFetchedPage: Boolean = true,
    val learnedHosts: LearnedHostStore? = InMemoryLearnedHostStore(),
    val listener: ResolutionListener? = null,
    val scoring: ScoringPolicy = ScoringPolicy(),
) {
    init {
        require(maxNavigationHops in 1..10) { "maxNavigationHops must be between 1 and 10" }
        require(maxCandidateAttempts in 1..10) { "maxCandidateAttempts must be between 1 and 10" }
        require(maxBodyChars in 1_024..16_000_000) { "maxBodyChars must be between 1 KB and 16 MB" }
    }
}

/**
 * Stores hostnames that the fallback identified and that were confirmed by a successful extraction.
 * Implementations may persist entries; they must be safe to call from several coroutines.
 */
interface LearnedHostStore {
    /** The server name learned for [host], or `null`. */
    fun serverFor(host: String): String?

    /** Records that [host] is served by [serverName]. */
    fun remember(host: String, serverName: String)

    /** Forgets [host], for example after an extraction on a learned host failed. */
    fun forget(host: String)
}

/** A bounded, thread-safe, in-memory [LearnedHostStore]. The oldest entry is evicted first. */
class InMemoryLearnedHostStore(private val maxEntries: Int = 256) : LearnedHostStore {
    private val entries = AtomicReference<Map<String, String>>(emptyMap())

    init {
        require(maxEntries > 0) { "maxEntries must be positive" }
    }

    override fun serverFor(host: String): String? = entries.load()[host.lowercase()]

    override fun remember(host: String, serverName: String) = update { current ->
        val key = host.lowercase()
        val next = LinkedHashMap(current).apply { remove(key); put(key, serverName) }
        while (next.size > maxEntries) next.remove(next.keys.first())
        next
    }

    override fun forget(host: String) = update { it - host.lowercase() }

    /** A snapshot of all learned entries, oldest first. */
    fun snapshot(): Map<String, String> = entries.load()

    private inline fun update(transform: (Map<String, String>) -> Map<String, String>) {
        while (true) {
            val current = entries.load()
            if (entries.compareAndSet(current, transform(current))) return
        }
    }
}

/** Receives [ResolutionEvent]s. Called synchronously; keep it fast and never throw. */
fun interface ResolutionListener {
    fun onEvent(event: ResolutionEvent)
}

/** Something worth reporting about how a URL was resolved. */
sealed class ResolutionEvent {
    abstract val url: String

    /**
     * A host that no server lists was identified and the identified server's extractor returned videos.
     * This is the signal to add [host] to that server's host list.
     */
    data class UnknownHostConfirmed(
        override val url: String,
        val host: String,
        val serverName: String,
        val stage: ResolutionStage,
        val evidence: List<String>,
    ) : ResolutionEvent()

    /** A learned host stopped working and was forgotten. */
    data class LearnedHostRejected(override val url: String, val host: String, val serverName: String) : ResolutionEvent()

    /** The URL could not be resolved. */
    data class Unresolved(override val url: String, val reason: UnresolvedReason, val evidence: List<String>) : ResolutionEvent()
}
