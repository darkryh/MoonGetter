package com.ead.lib.moongetter.utils

/**
 * Utility for debugging and analyzing extraction patterns and content.
 * Helps developers troubleshoot scraping issues.
 */
object ScrapingDebugger {

    /**
     * Analyzes HTML/text content and provides statistics useful for debugging.
     *
     * @param content The content to analyze.
     * @return Debug information about the content.
     */
    fun analyzeContent(content: String): ContentAnalysis {
        // Cache lowercase content for efficient case-insensitive checks
        val lowerContent = content.lowercase()
        
        return ContentAnalysis(
            length = content.length,
            lineCount = content.lines().size,
            hasScriptTags = lowerContent.contains("<script"),
            hasIframeTags = lowerContent.contains("<iframe"),
            hasVideoTags = lowerContent.contains("<video"),
            hasSourceTags = lowerContent.contains("<source"),
            hasM3u8References = lowerContent.contains(".m3u8"),
            hasMp4References = lowerContent.contains(".mp4"),
            containsPackedJs = lowerContent.contains("eval(function(p,a,c,k,e,d)")
        )
    }

    /**
     * Data class containing analysis results of content.
     */
    data class ContentAnalysis(
        val length: Int,
        val lineCount: Int,
        val hasScriptTags: Boolean,
        val hasIframeTags: Boolean,
        val hasVideoTags: Boolean,
        val hasSourceTags: Boolean,
        val hasM3u8References: Boolean,
        val hasMp4References: Boolean,
        val containsPackedJs: Boolean
    ) {
        override fun toString(): String = buildString {
            appendLine("Content Analysis:")
            appendLine("  Length: $length chars")
            appendLine("  Lines: $lineCount")
            appendLine("  Has <script>: $hasScriptTags")
            appendLine("  Has <iframe>: $hasIframeTags")
            appendLine("  Has <video>: $hasVideoTags")
            appendLine("  Has <source>: $hasSourceTags")
            appendLine("  Has .m3u8: $hasM3u8References")
            appendLine("  Has .mp4: $hasMp4References")
            appendLine("  Has packed JS: $containsPackedJs")
        }
    }

    /**
     * Tests a regex pattern against content and provides detailed match information.
     *
     * @param content The content to test against.
     * @param pattern The regex pattern to test.
     * @param groupIndex The capture group to extract (default is 1).
     * @return Test results including matches and statistics.
     */
    fun testPattern(
        content: String,
        pattern: String,
        groupIndex: Int = 1
    ): PatternTestResult {
        return try {
            val regex = Regex(pattern, RegexOption.MULTILINE)
            val matches = regex.findAll(content).toList()
            val extractedValues = matches.mapNotNull { 
                it.groups.elementAtOrNull(groupIndex)?.value 
            }

            PatternTestResult(
                isValid = true,
                matchCount = matches.size,
                extractedValues = extractedValues,
                error = null
            )
        } catch (e: Exception) {
            PatternTestResult(
                isValid = false,
                matchCount = 0,
                extractedValues = emptyList(),
                error = e.message
            )
        }
    }

    /**
     * Result of pattern testing.
     */
    data class PatternTestResult(
        val isValid: Boolean,
        val matchCount: Int,
        val extractedValues: List<String>,
        val error: String?
    ) {
        override fun toString(): String = buildString {
            appendLine("Pattern Test Result:")
            appendLine("  Valid: $isValid")
            if (error != null) {
                appendLine("  Error: $error")
            } else {
                appendLine("  Matches: $matchCount")
                if (extractedValues.isNotEmpty()) {
                    appendLine("  Extracted values:")
                    extractedValues.take(5).forEach { value ->
                        appendLine("    - ${value.take(100)}")
                    }
                    if (extractedValues.size > 5) {
                        appendLine("    ... and ${extractedValues.size - 5} more")
                    }
                }
            }
        }
    }

    /**
     * Extracts and displays sample snippets around pattern matches for debugging.
     *
     * @param content The content to search in.
     * @param pattern The pattern to match.
     * @param contextLength Characters to include before and after the match (default: 50).
     * @return List of match snippets with context.
     */
    fun getMatchContexts(
        content: String,
        pattern: String,
        contextLength: Int = 50
    ): List<MatchContext> {
        val regex = Regex(pattern, RegexOption.MULTILINE)
        return regex.findAll(content).take(5).map { match ->
            val start = maxOf(0, match.range.first - contextLength)
            val end = minOf(content.length, match.range.last + contextLength + 1)
            
            MatchContext(
                beforeContext = content.substring(start, match.range.first),
                matchedText = match.value,
                afterContext = content.substring(match.range.last + 1, end),
                position = match.range.first
            )
        }.toList()
    }

    /**
     * Context information around a pattern match.
     */
    data class MatchContext(
        val beforeContext: String,
        val matchedText: String,
        val afterContext: String,
        val position: Int
    ) {
        override fun toString(): String {
            return "...${beforeContext}[${matchedText}]${afterContext}... (pos: $position)"
        }
    }

    /**
     * Sanitizes content for safe logging by removing sensitive data.
     *
     * @param content The content to sanitize.
     * @param maxLength Maximum length to keep (default: 500).
     * @return Sanitized content safe for logging.
     */
    fun sanitizeForLog(content: String, maxLength: Int = 500): String {
        var sanitized = content
            .replace(Regex("token=[^&\\s]+"), "token=***")
            .replace(Regex("api_key=[^&\\s]+"), "api_key=***")
            .replace(Regex("key=[^&\\s]+"), "key=***")

        if (sanitized.length > maxLength) {
            sanitized = sanitized.take(maxLength) + "... (truncated)"
        }

        return sanitized
    }
}
