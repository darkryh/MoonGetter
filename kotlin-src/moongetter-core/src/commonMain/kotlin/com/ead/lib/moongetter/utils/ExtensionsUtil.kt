@file:Suppress("unused")

package com.ead.lib.moongetter.utils

import io.ktor.http.Url


/**
 * Default headers equivalent to Accept:
 */
private val DEFAULT_HEADERS: Map<String, String> = mapOf("Accept" to "*/*")
fun String.toHttpUrl(): Url = Url(this)