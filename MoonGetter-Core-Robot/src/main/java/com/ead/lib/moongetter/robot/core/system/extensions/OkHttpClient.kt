package com.ead.lib.moongetter.robot.core.system.extensions

import com.ead.lib.moongetter.core.system.extensions.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

suspend fun OkHttpClient.onResponse(url : String) : Response? {
    return try {
        OkHttpClient()
            .newCall(
                Request.Builder()
                    .url(url)
                    .build())
            .await()
    }
    catch (e: IllegalArgumentException) {
        e.printStackTrace()
        return null
    }
}