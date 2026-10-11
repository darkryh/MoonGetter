package com.ead.lib.moongetter.resolution

/**
 * The hostnames a server accepts, matched against the whole normalized hostname only.
 *
 * - [exact]: concrete hostnames, for example `voe.sx`.
 * - [subdomainsOf]: apex domains whose every subdomain is accepted, for example `rpmstream.live`
 *   accepts `gf21.rpmstream.live`. The apex itself is accepted too.
 * - [families]: bounded regular expressions for rotating names, matched against the entire hostname,
 *   for example `voeunblock(?:[1-9]|10)?\.com`. Never use unbounded wildcards here; a family must not
 *   be able to match another service's hostnames.
 *
 * A single leading `www.` is always accepted.
 */
class HostSet(
    exact: Collection<String> = emptyList(),
    subdomainsOf: Collection<String> = emptyList(),
    families: Collection<String> = emptyList(),
) {
    val exact: Set<String> = exact.map { normalize(it) }.toSet()
    val subdomainsOf: Set<String> = subdomainsOf.map { normalize(it) }.toSet()
    val families: List<String> = families.toList()

    private val familyRegexes = this.families.map { Regex(it) }

    init {
        require(this.exact.isNotEmpty() || this.subdomainsOf.isNotEmpty() || this.families.isNotEmpty()) {
            "A HostSet needs at least one host"
        }
        (this.exact + this.subdomainsOf).forEach {
            require(EmbedUrl.parse("https://$it/") != null) { "Invalid hostname in HostSet: $it" }
        }
    }

    /** True when [host] (already normalized by [EmbedUrl]) belongs to this set. */
    fun contains(host: String): Boolean {
        val candidate = normalize(host).removePrefix("www.")
        if (candidate in exact) return true
        if (subdomainsOf.any { candidate == it || candidate.endsWith(".$it") }) return true
        return familyRegexes.any { it.matches(candidate) }
    }

    /** Regular-expression source equivalent to [contains], including the optional `www.` prefix. */
    fun toPatternSource(): String {
        val alternatives = buildList {
            exact.sorted().forEach { add(escape(it)) }
            subdomainsOf.sorted().forEach { add("""(?:[a-z0-9-]+\.)*${escape(it)}""") }
            families.forEach { add("(?:$it)") }
        }
        return """(?:www\.)?(?:${alternatives.joinToString("|")})"""
    }

    private fun normalize(value: String) = value.lowercase().removeSuffix(".")

    private fun escape(host: String) = host.replace(".", "\\.")
}
