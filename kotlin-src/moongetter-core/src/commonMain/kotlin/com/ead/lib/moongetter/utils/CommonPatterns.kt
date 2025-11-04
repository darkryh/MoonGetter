package com.ead.lib.moongetter.utils

/**
 * Collection of common regex patterns used across different server implementations.
 * Helps standardize pattern usage and makes it easier to update patterns across servers.
 */
object CommonPatterns {

    /**
     * Patterns for extracting video URLs
     */
    object Video {
        /** 
         * Matches MP4 video URLs with full query parameter support.
         * Includes all URL-safe characters including &, =, and other query param characters.
         */
        const val MP4_URL = """(https?://[^\s"'<>]+\.mp4[^\s"'<>]*)"""
        
        /** 
         * Matches M3U8 playlist URLs with full query parameter support.
         * Includes all URL-safe characters including &, =, and other query param characters.
         */
        const val M3U8_URL = """(https?://[^\s"'<>]+\.m3u8[^\s"'<>]*)"""
        
        /** Matches video source tags */
        const val SOURCE_TAG = """<source[^>]*src=["']([^"']+)["'][^>]*>"""
        
        /** Matches video tags */
        const val VIDEO_TAG = """<video[^>]*src=["']([^"']+)["'][^>]*>"""
    }

    /**
     * Patterns for extracting iframe sources
     */
    object IFrame {
        /** Standard iframe src attribute */
        const val SRC = """<iframe[^>]*src=["']([^"']+)["'][^>]*>"""
        
        /** Data-src attribute (lazy loading) */
        const val DATA_SRC = """<iframe[^>]*data-src=["']([^"']+)["'][^>]*>"""
    }

    /**
     * Patterns for JavaScript extraction
     */
    object JavaScript {
        /** Matches packed JavaScript code */
        const val PACKED = """eval\(function\(p,a,c,k,e,d\).*?\}\(([^)]+)\)"""
        
        /** Matches variable assignments */
        fun variableAssignment(varName: String) = """var\s+$varName\s*=\s*["']([^"']+)["']"""
        
        /** Matches JSON object assignments */
        fun jsonAssignment(varName: String) = """var\s+$varName\s*=\s*(\{[^}]+\})"""
    }

    /**
     * Patterns for URL components
     */
    object Url {
        /** Extracts domain from URL */
        const val DOMAIN = """https?://([^/]+)"""
        
        /** Extracts query parameter value */
        fun queryParam(paramName: String) = """[?&]$paramName=([^&]+)"""
        
        /** Extracts URL from various contexts */
        const val ANY_URL = """(https?://[^\s"'<>]+)"""
    }

    /**
     * Patterns for tokens and keys
     */
    object Auth {
        /** Common token pattern */
        const val TOKEN = """token["']?\s*[:=]\s*["']?([a-zA-Z0-9_-]+)["']?"""
        
        /** API key pattern */
        const val API_KEY = """api_?key["']?\s*[:=]\s*["']?([a-zA-Z0-9_-]+)["']?"""
    }

    /**
     * Helper function to build a pattern that tries multiple alternatives.
     */
    fun alternatives(vararg patterns: String): String {
        return patterns.joinToString("|") { "($it)" }
    }

    /**
     * Helper function to make a pattern optional (matches 0 or 1 times).
     */
    fun optional(pattern: String): String {
        return "(?:$pattern)?"
    }

    /**
     * Helper function to create a pattern that matches content between two strings.
     */
    fun between(start: String, end: String, content: String = ".*?"): String {
        return """${Regex.escape(start)}($content)${Regex.escape(end)}"""
    }
}
