package com.ead.lib.moongetter.client.ktor.util

import io.ktor.http.Headers

internal fun Headers.toHashMap(): HashMap<String, String> {
    return HashMap<String, String>().apply {
        this@toHashMap.entries().forEach { (key, values) ->
            put(key, values.firstOrNull().orEmpty())
        }
    }
}