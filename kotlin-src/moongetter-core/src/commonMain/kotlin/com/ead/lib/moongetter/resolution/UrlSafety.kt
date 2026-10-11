package com.ead.lib.moongetter.resolution

/**
 * Decides whether the fallback may fetch a URL.
 *
 * The fallback fetches URLs the caller supplied, which on a server deployment is a request-forgery
 * risk. Only HTTPS on the default port to a public-looking hostname is allowed. IP literals,
 * single-label names and internal suffixes are refused.
 *
 * Limitation: names are not resolved here, so a public name pointing at a private address cannot be
 * detected. Deployments that need that guarantee must also restrict egress at the network level.
 */
object UrlSafety {

    private val internalSuffixes = listOf(
        "localhost", "local", "localdomain", "internal", "intranet", "lan", "home", "corp", "test", "invalid", "example",
        "home.arpa", "in-addr.arpa", "ip6.arpa", "onion",
    )

    /** `null` when [url] may be fetched, otherwise the reason it may not. */
    fun rejectionReason(url: EmbedUrl): String? = when {
        url.scheme != "https" -> "only HTTPS URLs are fetched"
        url.port != null && url.port != 443 -> "only the default HTTPS port is fetched"
        url.isIpLiteral -> "IP-literal hosts are not fetched"
        '.' !in url.host -> "single-label hosts are not fetched"
        internalSuffixes.any { url.host == it || url.host.endsWith(".$it") } -> "internal hostnames are not fetched"
        else -> null
    }

    /** True when [url] may be fetched. */
    fun isFetchable(url: EmbedUrl): Boolean = rejectionReason(url) == null
}
