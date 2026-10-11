package com.ead.lib.moongetter.resolution

/**
 * Structured routing data for a server, replacing a single URL regular expression.
 *
 * Hosts and path shapes are kept separate so that:
 * - known hosts are matched on the whole hostname only (no lookalike or query-string matches);
 * - a server can read the video ID from any host once the fallback has identified it;
 * - the fallback can use [fingerprint] and [family] to identify a page served from an unknown host.
 *
 * @property hosts Hostnames routed to this server without any network request.
 * @property paths Accepted shapes for the URL remainder; the first matching rule supplies the ID.
 * @property fingerprint HTML evidence identifying this server's pages, or `null` when it has none.
 * @property family Shared software this server runs on, when its pages are not unique on their own.
 * @property allowHttp Whether `http://` URLs are accepted in addition to `https://`.
 */
class ServerDescriptor(
    val hosts: HostSet,
    val paths: List<PathRule>,
    val fingerprint: Fingerprint? = null,
    val family: ServerFamily? = null,
    val allowHttp: Boolean = false,
) {
    init {
        require(paths.isNotEmpty()) { "A ServerDescriptor needs at least one PathRule" }
    }

    /** Matches a known-host URL: scheme, host and path shape must all be accepted. */
    fun match(url: EmbedUrl): Match? {
        if (!acceptsScheme(url) || !url.usesDefaultPort || !hosts.contains(url.host)) return null
        return matchPath(url)
    }

    /** Matches a URL string against known hosts; `null` when it does not parse or match. */
    fun match(url: String): Match? = EmbedUrl.parse(url)?.let(::match)

    /** Matches only the path shape, ignoring the host. Used once a server was identified by other evidence. */
    fun matchPath(url: EmbedUrl): Match? {
        if (!acceptsScheme(url)) return null
        for (rule in paths) {
            val id = rule.idOf(url.rest) ?: continue
            return Match(url, id, rule)
        }
        return null
    }

    /** Matches only the path shape of a URL string, ignoring the host. */
    fun matchPath(url: String): Match? = EmbedUrl.parse(url)?.let(::matchPath)

    /**
     * A regular expression equivalent to [match] for URLs with a lowercase scheme, for code that still
     * consumes [com.ead.lib.moongetter.models.Server.Factory.pattern]. The hostname part is case-insensitive.
     */
    val pattern: String by lazy {
        val host = """(?i:${hosts.toPatternSource()})\.?"""
        val authority = if (allowHttp) """(?:https://$host(?::443)?|http://$host(?::80)?)""" else """https://$host(?::443)?"""
        val rest = paths.joinToString("|") { "(?:${it.pattern})" }
        """^$authority(?:$rest)$"""
    }

    private fun acceptsScheme(url: EmbedUrl) = url.scheme == "https" || (allowHttp && url.scheme == "http")

    /** A successful match: the parsed URL, the video ID and the rule that produced it. */
    data class Match(val url: EmbedUrl, val id: String, val rule: PathRule)
}
