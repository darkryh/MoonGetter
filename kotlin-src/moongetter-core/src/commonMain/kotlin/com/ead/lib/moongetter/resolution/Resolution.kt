package com.ead.lib.moongetter.resolution

import com.ead.lib.moongetter.models.Server

/** The step that selected a server, from cheapest to most expensive. */
enum class ResolutionStage {
    /** The URL's host and path are listed by exactly one server. No network request. */
    KNOWN_HOST,

    /** The host was learned from an earlier confirmed fallback. No network request. */
    LEARNED_HOST,

    /** The URL redirected (HTTP, script or meta refresh) to a known host. */
    REDIRECT,

    /** The page embeds a known host in an iframe; that frame URL is used. */
    EMBEDDED_FRAME,

    /** Unique HTML evidence identified exactly one server. */
    FINGERPRINT,

    /** HTML identified a server family; members are tried until one extracts videos. */
    FAMILY_PROBE,
}

/** Why a URL could not be resolved. */
enum class UnresolvedReason {
    /** Not a well-formed absolute URL. */
    INVALID_URL,

    /** No known host and the fallback is disabled. */
    NO_KNOWN_HOST,

    /** More than one server claims the URL. This is a configuration error in the server list. */
    CONFLICTING_SERVERS,

    /** The fallback refused to fetch the URL (not HTTPS, private address, non-default port…). */
    UNSAFE_URL,

    /** The fallback request failed. */
    FETCH_FAILED,

    /** The fetched page is a bot-protection challenge. */
    CHALLENGE,

    /** The domain is parked or for sale. */
    PARKED,

    /** The file was deleted or does not exist. */
    NOT_FOUND,

    /** The fetched page is empty. */
    EMPTY_PAGE,

    /** Several servers have comparable evidence; none was chosen. */
    AMBIGUOUS,

    /** The page carries no usable evidence for any server. */
    NO_EVIDENCE,
}

/** One server the resolver proposes, with the URL it should receive. */
data class ResolutionCandidate(
    val factory: Server.Factory,
    /** The URL handed to the server: the input, a redirect target or a frame URL. */
    val url: String,
)

/** The resolver's answer for one URL. */
sealed class Resolution {
    abstract val url: String
    abstract val evidence: List<String>

    /**
     * Servers to try, in order. For every stage except [ResolutionStage.FAMILY_PROBE] there is exactly one.
     * A fallback answer is only final once a candidate's extractor returns videos.
     */
    data class Resolved(
        override val url: String,
        val stage: ResolutionStage,
        val candidates: List<ResolutionCandidate>,
        override val evidence: List<String>,
        /** The fetched page that produced this answer, when the fallback ran. */
        val page: PageSnapshot? = null,
        /** The input host, to be remembered if a candidate is confirmed. `null` when nothing should be learned. */
        val learnableHost: String? = null,
    ) : Resolution() {
        init {
            require(candidates.isNotEmpty()) { "A resolution needs at least one candidate" }
            require(stage == ResolutionStage.FAMILY_PROBE || candidates.size == 1) { "Only family probing has several candidates" }
        }

        /** The first (or only) candidate. */
        val primary: ResolutionCandidate get() = candidates.first()
    }

    /** No server was selected. [suspects] lists servers with partial evidence, best first. */
    data class Unresolved(
        override val url: String,
        val reason: UnresolvedReason,
        override val evidence: List<String>,
        val suspects: List<String> = emptyList(),
    ) : Resolution()
}
