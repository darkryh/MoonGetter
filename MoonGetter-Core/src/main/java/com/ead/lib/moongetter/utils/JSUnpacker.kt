package com.ead.lib.moongetter.utils

import kotlin.math.pow

object JSUnpacker {

    /**
     * Detects whether the JavaScript is P.A.C.K.E.R. encoded.
     * @param packedJS The packed JavaScript string.
     * @return true if it's P.A.C.K.E.R. encoded.
     */
    fun detect(packedJS: String?): Boolean {
        return if (packedJS != null) {
            val js = packedJS.replace(" ", "")
            val pattern = Regex("eval\\(function\\(p,a,c,k,e,[rd]")
            pattern.containsMatchIn(js)
        } else false
    }

    /**
     * Unpacks the JavaScript
     * @param packedJS The packed JavaScript string.
     * @return the unpacked JavaScript or null if unable to unpack.
     */
    fun unpack(packedJS: String?): String? {
        val js = packedJS ?: return null
        try {
            val pattern = Regex("\\}\\s*\\('(.*)',\\s*(.*?),\\s*(\\d+),\\s*'(.*?)'\\.split\\('\\|'\\)", RegexOption.DOT_MATCHES_ALL)
            val match = pattern.find(js) ?: return null

            val (payload, radixStr, countStr, symtabStr) = match.destructured
            val symtab = symtabStr.split("|").toTypedArray()

            val radix = radixStr.toIntOrNull() ?: 36
            val count = countStr.toIntOrNull() ?: 0

            if (symtab.size != count) {
                throw Exception("Unknown P.A.C.K.E.R. encoding")
            }

            val wordPattern = Regex("\\b\\w+\\b")
            val decoded = StringBuilder(payload)
            var replaceOffset = 0

            wordPattern.findAll(payload).forEach { m ->
                val word = m.value
                val x = Unbase.unbase(word, radix)
                val value = symtab.getOrNull(x)

                value?.let {
                    decoded.replace(m.range.first + replaceOffset, m.range.last + 1 + replaceOffset, it)
                    replaceOffset += it.length - word.length
                }
            }

            return decoded.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

private object Unbase {
    private const val ALPHABET_62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val ALPHABET_95 = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"

    fun unbase(str: String, radix: Int): Int {
        val alphabet: String? = when (radix) {
            62 -> ALPHABET_62
            95 -> ALPHABET_95
            in 37..62 -> ALPHABET_62.substring(0, radix)
            in 63..95 -> ALPHABET_95.substring(0, radix)
            else -> null
        }

        if (alphabet == null) {
            return str.toInt(radix)
        }

        val dictionary = alphabet.withIndex().associate { (i, c) -> c.toString() to i }

        return str.reversed().foldIndexed(0) { i, acc, char ->
            acc + (dictionary[char.toString()] ?: 0) * radix.toDouble().pow(i).toInt()
        }
    }
}