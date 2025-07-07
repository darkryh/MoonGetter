@file:Suppress("unused")

package com.ead.lib.moongetter.utils

import java.net.URL


/**
 * Default headers equivalent to Accept:
 */
private val DEFAULT_HEADERS: Map<String, String> = mapOf("Accept" to "*/*")
fun String.toHttpUrl(): URL = URL(this)