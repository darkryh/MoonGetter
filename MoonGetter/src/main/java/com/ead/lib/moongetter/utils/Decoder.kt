package com.ead.lib.moongetter.utils

fun decoder(encodedString: String): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    val output = StringBuilder()

    var i = 0
    while (i < encodedString.length) {
        val index1 = chars.indexOf(encodedString[i++])
        val index2 = chars.indexOf(encodedString[i++])
        val index3 = chars.indexOf(encodedString[i++])
        val index4 = chars.indexOf(encodedString[i++])

        val bits = (index1 shl 18) or (index2 shl 12) or (index3 shl 6) or index4

        output.append(((bits shr 16) and 0xff).toChar())
        if (index3 != 64) {
            output.append(((bits shr 8) and 0xff).toChar())
        }
        if (index4 != 64) {
            output.append((bits and 0xff).toChar())
        }
    }

    return output.toString()
}