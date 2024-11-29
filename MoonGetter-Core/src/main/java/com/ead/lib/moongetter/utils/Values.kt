@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.utils

import org.jetbrains.annotations.VisibleForTesting

object Values {
    const val DEBUG_ERROR = "MOON_ERROR"

    @VisibleForTesting
    var targetUrl : String? = null

    @VisibleForTesting
    var targetUrl2 : String? = null

    fun restoreValues() {
        targetUrl = null
        targetUrl2 = null
    }
}