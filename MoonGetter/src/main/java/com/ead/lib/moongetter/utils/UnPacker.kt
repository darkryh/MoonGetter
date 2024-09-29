package com.ead.lib.moongetter.utils

import java.util.regex.Pattern

object UnPacker {

    private val jsPattern = Pattern.compile("""\)\('(.*)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""", Pattern.DOTALL)

    fun isPacked(source: String): Boolean {
        return source.contains("eval(function(p,a,c,k,e,") || source.contains("eval(function(p,a,c,k,e,r)")
    }

    fun unpack(source: String): String? {
        if (!isPacked(source)) return source

        val matcher = jsPattern.matcher(source)
        if (matcher.find()) {
            val payload = matcher.group(1) ?: return null
            val radixStr = matcher.group(2)
            val countStr = matcher.group(3)
            val symtab = matcher.group(4)?.split("|") ?: return null

            val radix = radixStr?.toInt() ?: return null
            val count = countStr?.toInt() ?: return null

            return unpackPayload(payload, symtab, radix, count)
        }
        return null
    }

    private fun unpackPayload(payload: String, symtab: List<String>, radix: Int, count: Int): String {
        var result = payload
        for (i in count - 1 downTo 0) {
            if (i < symtab.size && symtab[i].isNotEmpty()) {
                result = result.replace("\\b${Integer.toString(i, radix)}\\b".toRegex(), symtab[i])
            }
        }
        return result
    }
}