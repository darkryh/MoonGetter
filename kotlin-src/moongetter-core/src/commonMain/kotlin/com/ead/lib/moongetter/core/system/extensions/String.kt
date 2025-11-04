@file:Suppress("unused")

package com.ead.lib.moongetter.core.system.extensions

import com.ead.lib.moongetter.utils.PatternManager

fun String.delete(string: String) : String =
    this.replace(string,"")

fun String.replaceDomainWith(domain: String) : String? {
    val domainToReplace = PatternManager.singleMatch(
        string = this,
        regex = """https?://([a-zA-Z0-9\-]+\.[a-zA-Z]{2,})(/[^\s]*)?"""
    )

    return domainToReplace?.let {
        this.replace(domainToReplace, domain)
    }
}

/**
 * Extracts first match using a regex pattern.
 */
fun String.extractFirst(regex: String, groupIndex: Int = 1): String? {
    return PatternManager.singleMatch(this, regex, groupIndex)
}

/**
 * Extracts all matches using a regex pattern.
 */
fun String.extractAll(regex: String, groupIndex: Int = 1): List<String> {
    return PatternManager.findMultipleMatches(this, regex, groupIndex)
}

/**
 * Tries multiple patterns and returns the first match.
 */
fun String.extractWithFallback(vararg patterns: String, groupIndex: Int = 1): String? {
    return PatternManager.tryMultiplePatterns(this, patterns.toList(), groupIndex)
}

/**
 * Checks if the string matches any of the given patterns.
 */
fun String.matchesAny(vararg patterns: String): Boolean {
    return patterns.any { pattern ->
        PatternManager.match(pattern, this)
    }
}

/**
 * Extracts a URL and handles relative/absolute URL resolution.
 */
fun String.extractUrl(regex: String, baseUrl: String? = null, groupIndex: Int = 1): String? {
    return PatternManager.extractUrl(this, regex, baseUrl, groupIndex)
}

/**
 * Extracts all capture groups from first match.
 */
fun String.extractGroups(regex: String): List<String> {
    return PatternManager.extractAllGroups(this, regex)
}

/**
 * Cleans HTML tags from string.
 */
fun String.stripHtmlTags(): String {
    return this.replace(Regex("<[^>]*>"), "")
}

/**
 * Decodes HTML entities (basic implementation).
 */
fun String.decodeHtmlEntities(): String {
    return this
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
}

/**
 * Extracts JSON value by key from a simple JSON string.
 */
fun String.extractJsonValue(key: String): String? {
    val pattern = """"$key"\s*:\s*"([^"]+)""""
    return PatternManager.singleMatch(this, pattern)
}

/**
 * Checks if string contains a video file extension.
 */
fun String.hasVideoExtension(): Boolean {
    val extensions = listOf(".mp4", ".m3u8", ".mpd", ".avi", ".mkv", ".webm")
    return extensions.any { this.contains(it, ignoreCase = true) }
}
