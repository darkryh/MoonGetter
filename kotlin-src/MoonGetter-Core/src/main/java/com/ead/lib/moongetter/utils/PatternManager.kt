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
        return Regex(regex, RegexOption.DOT_MATCHES_ALL)
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
}