@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.utils

object Values {
    const val DEBUG_ERROR = "MOON_ERROR"

    var targetUrl : String? = null

    var targetUrl2 : String? = null

    fun restoreValues() {
        targetUrl = null
        targetUrl2 = null
    }
}