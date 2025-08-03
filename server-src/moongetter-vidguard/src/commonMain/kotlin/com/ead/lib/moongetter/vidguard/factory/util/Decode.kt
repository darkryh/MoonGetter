package com.ead.lib.moongetter.vidguard.factory.util

import io.ktor.utils.io.core.toByteArray
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun decodeObstructionData(url: String): String {
    val obstructedData = url.substringAfter("sig=").substringBefore("&")

    val hexDecoded = obstructedData.chunked(2)
        .joinToString("") { (it.toInt(16) xor 2).toChar().toString() }

    val padded = when (hexDecoded.length % 4) {
        2 -> "$hexDecoded=="
        3 -> "$hexDecoded="
        else -> hexDecoded
    }

    val decoded = Base64.decode(padded.toByteArray())
        .decodeToString()

    val reversed = decoded.dropLast(5).reversed().toCharArray()

    for (i in reversed.indices step 2) {
        if (i + 1 < reversed.size) {
            val tmp = reversed[i]
            reversed[i] = reversed[i + 1]
            reversed[i + 1] = tmp
        }
    }

    val finalData = reversed.concatToString().dropLast(5)

    return url.replace(obstructedData, finalData)
}