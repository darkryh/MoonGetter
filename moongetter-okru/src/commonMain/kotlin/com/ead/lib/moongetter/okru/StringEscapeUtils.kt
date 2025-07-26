package com.ead.lib.moongetter.okru

object StringEscapeUtils {
    private val htmlEntities = mapOf(
        "&lt;" to "<",
        "&gt;" to ">",
        "&amp;" to "&",
        "&quot;" to "\"",
        "&apos;" to "'",
        // Agrega más entidades si es necesario
    )

    fun unescapeHtml4(input: String): String {
        var result = input
        for ((entity, char) in htmlEntities) {
            result = result.replace(entity, char)
        }
        return result
    }
}