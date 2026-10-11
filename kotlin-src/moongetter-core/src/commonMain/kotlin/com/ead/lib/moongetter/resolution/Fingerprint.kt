package com.ead.lib.moongetter.resolution

/**
 * How strongly a [Signal] points at one server.
 */
enum class SignalKind {
    /** Observed only on this server's pages. One unique signal can identify a server on its own. */
    UNIQUE,

    /** Consistent with this server but also plausible elsewhere. Adds confidence, never decides alone. */
    SUPPORTING,

    /** Shared by every member of a [ServerFamily]. Identifies the family, not the member. */
    FAMILY,
}

/** Which part of a fetched page a [Signal] inspects. */
enum class SignalScope {
    /** The whole (size-capped) HTML body. */
    BODY,

    /** The text of the first `<title>` element. */
    TITLE,

    /** The `src` attributes of `<script>` elements. */
    SCRIPT_SRC,

    /** Response header values, formatted as `name: value` lines with lowercase names. */
    HEADER,
}

/**
 * One piece of HTML evidence. Signals are plain text or regular-expression checks; page JavaScript is
 * never executed.
 */
class Signal private constructor(
    /** Stable identifier reported in resolution evidence, for example `mixdrop.mdcore`. */
    val id: String,
    val kind: SignalKind,
    val scope: SignalScope,
    private val regex: Regex,
) {
    /** True when this signal is present in [page]. */
    fun matches(page: PageSnapshot): Boolean = when (scope) {
        SignalScope.BODY -> regex.containsMatchIn(page.body)
        SignalScope.TITLE -> page.title?.let { regex.containsMatchIn(it) } ?: false
        SignalScope.SCRIPT_SRC -> page.scriptSources.any { regex.containsMatchIn(it) }
        SignalScope.HEADER -> regex.containsMatchIn(page.headerText)
    }

    override fun toString(): String = "$id($kind/$scope)"

    companion object {
        /** A literal, case-insensitive text check. */
        fun text(id: String, kind: SignalKind, scope: SignalScope, text: String): Signal {
            require(text.isNotBlank()) { "Signal text cannot be blank" }
            return Signal(id, kind, scope, Regex(Regex.escape(text), RegexOption.IGNORE_CASE))
        }

        /** A regular-expression check. Patterns are case-sensitive unless they say otherwise. */
        fun pattern(id: String, kind: SignalKind, scope: SignalScope, pattern: String): Signal =
            Signal(id, kind, scope, Regex(pattern))
    }
}

/** The HTML evidence identifying one server's pages. */
class Fingerprint(val signals: List<Signal>) {
    init {
        require(signals.none { it.kind == SignalKind.FAMILY }) {
            "Family signals belong to a ServerFamily, not to a member Fingerprint"
        }
        require(signals.map { it.id }.toSet().size == signals.size) { "Signal ids must be unique" }
    }

    constructor(vararg signals: Signal) : this(signals.toList())
}

/**
 * Shared software that several servers run on, whose pages look the same apart from branding.
 *
 * When a page carries at least [minimumHits] of the family [signals] but no member's unique signal,
 * the resolver cannot tell the members apart from HTML. It then tries the members' extractors in
 * ranked order and the first one that returns videos wins.
 */
class ServerFamily(
    val id: String,
    val signals: List<Signal>,
    val minimumHits: Int = 2,
) {
    init {
        require(signals.all { it.kind == SignalKind.FAMILY }) { "ServerFamily signals must be of kind FAMILY" }
        require(minimumHits in 1..signals.size) { "minimumHits must be between 1 and the number of signals" }
    }

    override fun equals(other: Any?): Boolean = other is ServerFamily && other.id == id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "ServerFamily($id)"

    companion object {
        /**
         * XFileSharing-based video hosts (Streamwish, Vidhide/FileLions, Lulustream and their clones). Their
         * players share the upload script, JW Player setup and Dean Edwards packed configuration.
         */
        val XFileSharing = ServerFamily(
            id = "xfilesharing",
            signals = listOf(
                Signal.pattern("xfs.xupload-script", SignalKind.FAMILY, SignalScope.SCRIPT_SRC, """(?:^|/)js/xupload\.js(?:[?#]|$)"""),
                Signal.pattern("xfs.packed-player", SignalKind.FAMILY, SignalScope.BODY, """eval\(function\(p,a,c,k,e,[dr]\)"""),
                Signal.pattern("xfs.vplayer", SignalKind.FAMILY, SignalScope.BODY, """\bvplayer\b"""),
                Signal.pattern("xfs.localstorage-slim", SignalKind.FAMILY, SignalScope.SCRIPT_SRC, """(?:^|/)js/localstorage-slim\.js(?:[?#]|$)"""),
            ),
            minimumHits = 2,
        )
    }
}

/**
 * A fetched page as seen by the fallback: final URL after redirects, status, headers and a size-capped body.
 */
class PageSnapshot(
    /** The URL that was requested. */
    val requestedUrl: String,
    /** The URL that produced this response after HTTP redirects. */
    val finalUrl: String,
    val statusCode: Int,
    headers: Map<String, String>,
    body: String,
    maxBodyChars: Int = DEFAULT_MAX_BODY_CHARS,
) {
    /** Response headers with lowercase names. */
    val headers: Map<String, String> = headers.mapKeys { it.key.lowercase() }

    /** The response body, truncated to `maxBodyChars`. */
    val body: String = if (body.length > maxBodyChars) body.substring(0, maxBodyChars) else body

    /** True when the body was longer than the cap. */
    val isTruncated: Boolean = body.length > maxBodyChars

    /** Text of the first `<title>` element, whitespace-collapsed and HTML-unescaped for common entities. */
    val title: String? by lazy {
        TITLE.find(this.body)?.groupValues?.get(1)
            ?.replace(Regex("""\s+"""), " ")
            ?.replace("&amp;", "&")?.replace("&#39;", "'")?.replace("&quot;", "\"")
            ?.trim()
    }

    /** `src` attribute values of `<script>` elements. */
    val scriptSources: List<String> by lazy {
        SCRIPT_TAG.findAll(this.body).mapNotNull { tag ->
            SRC_ATTRIBUTE.find(tag.groupValues[1])?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }
        }.toList()
    }

    internal val headerText: String by lazy { this.headers.entries.joinToString("\n") { "${it.key}: ${it.value}" } }

    companion object {
        const val DEFAULT_MAX_BODY_CHARS = 1_000_000

        private val TITLE = Regex("""<title[^>]*>([\s\S]*?)</title\s*>""", RegexOption.IGNORE_CASE)
        private val SCRIPT_TAG = Regex("""<script\b([^>]*)>""", RegexOption.IGNORE_CASE)
        private val SRC_ATTRIBUTE = Regex("""\bsrc\s*=\s*(?:"([^"]*)"|'([^']*)')""", RegexOption.IGNORE_CASE)
    }
}
