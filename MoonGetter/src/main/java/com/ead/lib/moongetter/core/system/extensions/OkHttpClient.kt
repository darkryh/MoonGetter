package com.ead.lib.moongetter.core.system.extensions

import okhttp3.OkHttpClient
import okhttp3.Response

suspend fun OkHttpClient.onNullableResponse(url : String) : Response? {
    return try {
        OkHttpClient()
            .newCall(
                okhttp3.Request.Builder()
                    .url(url)
                    .build())
            .await()
    }
    catch (e: IllegalArgumentException) {
        e.printStackTrace()
        return null
    }
}