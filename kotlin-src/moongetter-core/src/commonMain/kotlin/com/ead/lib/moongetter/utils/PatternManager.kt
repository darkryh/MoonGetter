package com.ead.lib.moongetter.utils

/**
 * Utility object for common Regex-based pattern matching operations.
 */
object PatternManager {

    /**
     * Finds the first match of the given [regex] pattern in the input [string]
     * and returns the value of the capture group at the specified [groupIndex].
     *
     * @param string The input string to search.
     * @param regex The regex pattern to apply.
     * @param groupIndex The capture group to return (default is 1).
     * @param patternFlag Regex flags such as [RegexOption.MULTILINE].
     * @return The matched group value, or `null` if no match was found.
     */
    fun singleMatch(
        string: String,
        regex: String,
        groupIndex: Int = 1,
        patternFlag: RegexOption = RegexOption.MULTILINE
    ): String? {
        val match = Regex(regex, patternFlag).find(string)
        return match?.groups?.get(groupIndex)?.value
    }

    /**
     * Finds all matches of the given [regex] in the input [string], and extracts
     * the specified capture group [groupIndex] from each match.
     *
     * @param string The input text to search through.
     * @param regex The regex pattern to use.
     * @param groupIndex The capture group index to extract (default is 1).
     * @param option Regex options such as [RegexOption.MULTILINE].
     * @return A list of non-null matching group values.
     */
    fun findMultipleMatches(
        string: String,
        regex: String,
        groupIndex: Int = 1,
        option: RegexOption = RegexOption.MULTILINE
    ): List<String> {
        return Regex(regex, option)
            .findAll(string)
            .mapNotNull { match: MatchResult ->
                match.groups.elementAtOrNull(groupIndex)?.value
            }
            .toList()
    }

    /**
     * Finds all matches of the given [regex] pattern in the input [string] and extracts
     * the first two capture groups as key-value pairs.
     *
     * Useful when the regex is designed to match structured content like key-value blocks.
     *
     * @param string The input text to search.
     * @param regex The regex with at least two capture groups.
     * @return A list of key-value [Pair]s extracted from each match.
     */
    fun findMultipleMatchesAsPairs(string: String, regex: String): List<Pair<String, String>> {
        return Regex(regex, RegexOption.MULTILINE)
            .findAll(string)
            .mapNotNull { match: MatchResult ->
                val key = match.groups.elementAtOrNull(1)?.value
                val value = match.groups.elementAtOrNull(2)?.value
                if (key != null && value != null) key to value else null
            }
            .toList()
    }

    /**
     * Checks whether the [string] contains at least one match of the given [regex] pattern.
     *
     * @param regex The pattern to check.
     * @param string The input text.
     * @param option Optional regex flags like [RegexOption.IGNORE_CASE].
     * @return `true` if a match is found, `false` otherwise.
     */
    fun match(regex: String, string: String, option: RegexOption = RegexOption.IGNORE_CASE): Boolean {
        return Regex(regex, option).containsMatchIn(string)
    }

    /**
     * Tries multiple regex patterns sequentially until one matches.
     * Useful when different servers may use different HTML structures.
     *
     * @param string The input text to search.
     * @param patterns List of regex patterns to try.
     * @param groupIndex The capture group to extract (default is 1).
     * @param option Regex options such as [RegexOption.MULTILINE].
     * @return The first successful match, or `null` if none match.
     */
    fun tryMultiplePatterns(
        string: String,
        patterns: List<String>,
        groupIndex: Int = 1,
        option: RegexOption = RegexOption.MULTILINE
    ): String? {
        for (pattern in patterns) {
            val result = singleMatch(string, pattern, groupIndex, option)
            if (result != null) return result
        }
        return null
    }

    /**
     * Extracts all capture groups from the first match of a regex pattern.
     *
     * @param string The input text to search.
     * @param regex The regex pattern to apply.
     * @param option Regex options such as [RegexOption.MULTILINE].
     * @return A list of all captured groups (excluding group 0 which is the full match), or empty list if no match.
     */
    fun extractAllGroups(
        string: String,
        regex: String,
        option: RegexOption = RegexOption.MULTILINE
    ): List<String> {
        val match = Regex(regex, option).find(string) ?: return emptyList()
        return match.groupValues.drop(1) // Skip the first element (full match)
    }

    /**
     * Finds all matches and returns all capture groups for each match.
     *
     * @param string The input text to search.
     * @param regex The regex pattern with multiple capture groups.
     * @param option Regex options such as [RegexOption.MULTILINE].
     * @return A list of lists, where each inner list contains the captured groups for one match.
     */
    fun findAllMatchesWithAllGroups(
        string: String,
        regex: String,
        option: RegexOption = RegexOption.MULTILINE
    ): List<List<String>> {
        return Regex(regex, option)
            .findAll(string)
            .map { match -> match.groupValues.drop(1) }
            .toList()
    }

    /**
     * Validates if a regex pattern is syntactically correct.
     *
     * @param pattern The regex pattern to validate.
     * @return `true` if the pattern is valid, `false` otherwise.
     */
    fun isValidPattern(pattern: String): Boolean {
        return try {
            Regex(pattern)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts a match and applies a transformation function to it.
     *
     * @param string The input text to search.
     * @param regex The regex pattern to apply.
     * @param groupIndex The capture group to extract (default is 1).
     * @param option Regex options such as [RegexOption.MULTILINE].
     * @param transform Function to transform the matched string.
     * @return The transformed result, or `null` if no match was found.
     */
    fun <T> extractAndTransform(
        string: String,
        regex: String,
        groupIndex: Int = 1,
        option: RegexOption = RegexOption.MULTILINE,
        transform: (String) -> T
    ): T? {
        val match = singleMatch(string, regex, groupIndex, option)
        return match?.let(transform)
    }

    /**
     * Extracts a URL from HTML content, handling both absolute and relative URLs.
     *
     * @param string The HTML content to search.
     * @param regex The regex pattern to match URLs.
     * @param baseUrl Optional base URL to resolve relative URLs.
     * @param groupIndex The capture group containing the URL (default is 1).
     * @return The extracted URL, properly resolved if relative, or `null` if no match.
     */
    fun extractUrl(
        string: String,
        regex: String,
        baseUrl: String? = null,
        groupIndex: Int = 1
    ): String? {
        val url = singleMatch(string, regex, groupIndex) ?: return null
        
        // If URL starts with http/https, return as-is
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        
        // If URL starts with //, prepend https:
        if (url.startsWith("//")) {
            return "https:$url"
        }
        
        // If base URL provided and URL is relative, resolve it
        if (baseUrl != null && !url.startsWith("http")) {
            val base = baseUrl.trimEnd('/')
            val path = url.trimStart('/')
            return "$base/$path"
        }
        
        return url
    }
}