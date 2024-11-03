@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.utils

import androidx.annotation.VisibleForTesting

object Values {
    const val DEBUG_ERROR = "MOON_ERROR"

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var targetUrl : String? = null

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var targetUrl2 : String? = null

    fun restoreValues() {
        targetUrl = null
        targetUrl2 = null
    }
}