package com.ead.lib.moongetter.resolution

/**
 * The shape of an embed URL after the hostname: path, query and fragment.
 *
 * [pattern] must match the entire remainder (it is anchored automatically) and its first
 * capture group must be the video ID. Examples: `/e/([A-Za-z0-9]+)/?(?:[?#]\S*)?` or
 * `/?#([A-Za-z0-9_-]{2,64})(?:&\S*)?`.
 *
 * [prior] is the evidence weight this shape contributes during fingerprint scoring when the host is
 * unknown. Generic shapes such as `/e/{id}` are shared by many services and deserve a low prior;
 * distinctive ones such as a fragment ID deserve more.
 */
class PathRule(
    val pattern: String,
    val prior: Int = GENERIC_PRIOR,
) {
    private val regex = Regex(pattern)

    init {
        require(prior >= 0) { "A path prior cannot be negative" }
    }

    /** Returns the video ID when [rest] (see [EmbedUrl.rest]) has this shape. */
    fun idOf(rest: String): String? = regex.matchEntire(rest)?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }

    companion object {
        /** Prior for shapes such as `/e/{id}` that many services share. */
        const val GENERIC_PRIOR = 5

        /** Prior for shapes that only a few services use. */
        const val DISTINCTIVE_PRIOR = 15
    }
}
