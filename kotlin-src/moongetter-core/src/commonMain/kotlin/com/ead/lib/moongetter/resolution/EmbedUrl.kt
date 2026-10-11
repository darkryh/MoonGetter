package com.ead.lib.moongetter.resolution

/**
 * A strictly parsed absolute URL used for server routing.
 *
 * Routing decisions are made on the normalized hostname and on the remainder of the URL
 * (path, query and fragment) separately, so a known host appearing in a query string or in
 * a lookalike domain can never select a server.
 *
 * Parsing rejects whitespace, user-info (`user@host`), IPv6 literals, empty or oversized
 * labels and non-ASCII hostnames. Hostnames are lowercased and a single trailing dot is removed.
 */
class EmbedUrl private constructor(
    /** The input exactly as supplied. */
    val raw: String,
    /** Lowercased scheme, for example `https`. */
    val scheme: String,
    /** Lowercased hostname without a trailing dot. */
    val host: String,
    /** Explicit port, or `null` when the URL uses the scheme default. */
    val port: Int?,
    /** Everything after the authority: path, query and fragment. Empty or starting with `/`, `?` or `#`. */
    val rest: String,
) {

    /** The path component, `""` when absent. */
    val path: String get() = rest.substringBefore('#').substringBefore('?')

    /** `scheme://host[:port]`. */
    val origin: String get() = "$scheme://$host" + (port?.let { ":$it" } ?: "")

    /** The hostname with one leading `www.` removed. */
    val hostWithoutWww: String get() = host.removePrefix("www.")

    /** True when the port is absent or the scheme default. */
    val usesDefaultPort: Boolean
        get() = port == null || (scheme == "https" && port == 443) || (scheme == "http" && port == 80)

    /** True when the host is an IPv4 literal. */
    val isIpLiteral: Boolean get() = IPV4.matches(host)

    /** Resolves [reference] (absolute, scheme-relative, absolute-path or relative) against this URL. */
    fun resolve(reference: String): EmbedUrl? {
        val value = reference.trim()
        if (value.isEmpty() || value.any { it.isWhitespace() }) return null
        return when {
            SCHEME.containsMatchIn(value) -> parse(value)
            value.startsWith("//") -> parse("$scheme:$value")
            value.startsWith("/") -> parse("$origin$value")
            value.startsWith("?") -> parse("$origin$path$value")
            value.startsWith("#") -> parse("$origin${rest.substringBefore('#')}$value")
            else -> parse("$origin${path.substringBeforeLast('/', "")}/$value")
        }
    }

    override fun toString(): String = "$origin$rest"

    override fun equals(other: Any?): Boolean = other is EmbedUrl && other.toString() == toString()

    override fun hashCode(): Int = toString().hashCode()

    companion object {
        private val SCHEME = Regex("""^[A-Za-z][A-Za-z0-9+.-]*://""")
        private val SHAPE = Regex("""^([A-Za-z][A-Za-z0-9+.-]*)://([^/?#]*)(.*)$""")
        private val LABEL = Regex("""^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$""")
        private val IPV4 = Regex("""^\d{1,3}(?:\.\d{1,3}){3}$""")

        /** Parses [value], returning `null` for anything that is not a well-formed absolute URL. */
        fun parse(value: String): EmbedUrl? {
            if (value.isEmpty() || value.length > 4096 || value.any { it.isWhitespace() || it.code < 0x20 }) return null
            val match = SHAPE.matchEntire(value) ?: return null
            val scheme = match.groupValues[1].lowercase()
            val authority = match.groupValues[2]
            val rest = match.groupValues[3]
            if (authority.isEmpty() || '@' in authority || authority.startsWith("[")) return null
            if (rest.isNotEmpty() && rest[0] !in "/?#") return null

            val hostPart = authority.substringBefore(':')
            val portPart = if (':' in authority) authority.substringAfter(':') else null
            val port = when {
                portPart == null -> null
                portPart.isEmpty() || portPart.length > 5 || portPart.any { !it.isDigit() } -> return null
                else -> portPart.toInt().takeIf { it in 1..65535 } ?: return null
            }
            if (hostPart.any { it.code > 0x7F }) return null
            val host = hostPart.lowercase().removeSuffix(".")
            if (host.isEmpty() || host.length > 253) return null
            val labels = host.split('.')
            if (labels.any { !LABEL.matches(it) }) return null
            return EmbedUrl(value, scheme, host, port, rest)
        }
    }
}
