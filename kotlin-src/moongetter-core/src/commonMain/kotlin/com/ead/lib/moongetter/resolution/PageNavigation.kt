package com.ead.lib.moongetter.resolution

/** How a page points at another URL. */
enum class NavigationKind {
    /** `window.location = '…'`, `location.replace('…')` and similar literal script navigations. */
    SCRIPT_REDIRECT,

    /** `<meta http-equiv="refresh" content="0;url=…">`. */
    META_REFRESH,

    /** `<iframe src="…">`: a wrapper page embedding the real player. */
    FRAME,
}

/** A literal navigation target found in a page. */
data class Navigation(val kind: NavigationKind, val target: EmbedUrl)

/**
 * Extracts literal navigation targets from HTML without executing it.
 *
 * Only string literals are read. Targets built by concatenation or templates (`${'$'}{slug}`) are ignored,
 * because their value cannot be known without running the page.
 */
object PageNavigation {

    private val scriptRedirects = listOf(
        Regex("""\b(?:window\.|document\.|top\.|self\.)?location(?:\.href)?\s*=\s*(["'])([^"'\s+`${'$'}]+)\1\s*(?:;|\n|\r|<|\})"""),
        Regex("""\b(?:window\.|document\.|top\.|self\.)?location\.(?:replace|assign)\(\s*(["'])([^"'\s+`${'$'}]+)\1\s*\)"""),
    )
    private val metaRefresh = Regex(
        """<meta\b[^>]*http-equiv\s*=\s*["']?refresh["']?[^>]*content\s*=\s*["']\s*\d+\s*;\s*url\s*=\s*([^"'\s>]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val frames = Regex("""<iframe\b[^>]*\bsrc\s*=\s*["']([^"'\s`${'$'}]+)["']""", RegexOption.IGNORE_CASE)

    /** Navigation targets in document order within each kind: script redirects, meta refresh, then frames. */
    fun find(page: PageSnapshot): List<Navigation> {
        val base = EmbedUrl.parse(page.finalUrl) ?: return emptyList()
        val found = linkedSetOf<Navigation>()
        for (regex in scriptRedirects) {
            regex.findAll(page.body).forEach { match ->
                base.resolve(match.groupValues[2])?.let { found += Navigation(NavigationKind.SCRIPT_REDIRECT, it) }
            }
        }
        metaRefresh.findAll(page.body).forEach { match ->
            base.resolve(match.groupValues[1].replace("&amp;", "&"))?.let { found += Navigation(NavigationKind.META_REFRESH, it) }
        }
        frames.findAll(page.body).forEach { match ->
            base.resolve(match.groupValues[1].replace("&amp;", "&"))?.let { found += Navigation(NavigationKind.FRAME, it) }
        }
        return found.filter { it.target.scheme == "https" || it.target.scheme == "http" }
            .sortedBy { it.kind.ordinal }
    }
}
