package com.ead.lib.moongetter.utils

import java.util.regex.Pattern

object PatternManager {

    fun singleMatch(string: String, regex : String, groupIndex : Int = 1, patternFlag : Int = Pattern.MULTILINE): String? {
        val pattern = Pattern.compile(regex, patternFlag)
        val matcher = pattern.matcher(string)
        return if (matcher.find()) {
            matcher.group(groupIndex)
        } else null
    }

    fun findMultipleMatches(string: String, regex: String, groupIndex: Int=1, patternFlag: Int = Pattern.MULTILINE): List<String> {
        val pattern = Pattern.compile(regex, patternFlag)
        val matcher = pattern.matcher(string)
        val stringArrayList = ArrayList<String>()
        while (matcher.find()) {
            matcher.group(groupIndex)?.let { stringArrayList.add(it) }
        }
        return stringArrayList
    }

    fun findMultipleMatchesAsPairs(string: String, regex: String): List<Pair<String, String>> {
        val pattern = Pattern.compile(regex, Pattern.DOTALL)
        val matcher = pattern.matcher(string)
        val matches = mutableListOf<Pair<String, String>>()

        while (matcher.find()) {
            val key = matcher.group(1)
            val value = matcher.group(2)
            if (key != null && value != null) {
                matches.add(Pair(key, value))
            }
        }
        return matches
    }

    fun match(regex: String, string: String): Boolean {
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(string)
        return matcher.find()
    }
}