package com.ead.lib.moongetter.resolution

/** What kind of page the fallback fetched, decided before any server scoring. */
enum class PageKind {
    /** A page that may belong to a video host: scoring continues. */
    CONTENT,

    /** A bot-protection interstitial (Cloudflare, DDoS-Guard). Its markup belongs to the protector. */
    CHALLENGE,

    /** A parked, for-sale or "coming soon" domain. */
    PARKED,

    /** The file was deleted or never existed. */
    NOT_FOUND,

    /** An empty body. */
    EMPTY,
}

/**
 * Classifies fetched pages so that interstitials and placeholder pages are never scored as players.
 *
 * Every rule here describes the page's *state*, not the server: a challenge page on a real Doodstream
 * mirror is still a challenge, and its Cloudflare markup must not be fingerprinted.
 */
object PageClassifier {

    private val challengeMarkers = listOf(
        Regex("""<title>\s*Just a moment\.\.\.\s*</title>""", RegexOption.IGNORE_CASE),
        Regex("""<title>\s*Attention Required!\s*\|\s*Cloudflare\s*</title>""", RegexOption.IGNORE_CASE),
        Regex("""\bcf-chl-[a-z-]+"""),
        Regex("""window\._cf_chl_opt\b"""),
        Regex("""<title>\s*DDoS-Guard\s*</title>""", RegexOption.IGNORE_CASE),
        Regex("""ddos-guard\.net/[^"']*check""", RegexOption.IGNORE_CASE),
    )

    private val parkedMarkers = listOf(
        Regex("""parklogic\.com""", RegexOption.IGNORE_CASE),
        Regex("""\bdomain (?:is|may be) for sale\b""", RegexOption.IGNORE_CASE),
        Regex("""\bbuy this domain\b""", RegexOption.IGNORE_CASE),
        Regex("""\bdomain is coming soon\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:sedoparking|bodis\.com|afternic\.com|hugedomains\.com|dan\.com/buy-domain)""", RegexOption.IGNORE_CASE),
        Regex("""<title>[^<]*\|\s*(?:ED|Domains)\.com\s*</title>""", RegexOption.IGNORE_CASE),
    )

    private val notFoundMarkers = listOf(
        Regex("""\bFile is no longer available\b""", RegexOption.IGNORE_CASE),
        Regex("""\bFile was deleted\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:video|file) (?:has been|was) (?:deleted|removed)\b""", RegexOption.IGNORE_CASE),
        Regex("""<title>\s*(?:File|Video) Not Found\s*</title>""", RegexOption.IGNORE_CASE),
    )

    /** Classifies [page]. Order matters: a challenge outranks everything because its body is not the host's. */
    fun classify(page: PageSnapshot): PageKind {
        val body = page.body
        return when {
            body.isBlank() -> PageKind.EMPTY
            challengeMarkers.any { it.containsMatchIn(body) } -> PageKind.CHALLENGE
            parkedMarkers.any { it.containsMatchIn(body) } -> PageKind.PARKED
            page.statusCode == 404 || page.statusCode == 410 -> PageKind.NOT_FOUND
            notFoundMarkers.any { it.containsMatchIn(body) } -> PageKind.NOT_FOUND
            else -> PageKind.CONTENT
        }
    }
}
