package com.ead.lib.moongetter.doodstream

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class Doodstream(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    private val hostTarget = url.toHttpUrl().host

    override suspend fun onExtract(): List<Video> {

        var response = client
            .newCall(GET())
            .await()

        var body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))

        val host = response.request.url.host

        val referer = this@Doodstream.url.replace(hostTarget, host)

        val keysCode = PatternManager.singleMatch(
            string = body,
            regex =  "dsplayer\\.hotkeys[^']+'([^']+).+?function"
        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))

        val token = PatternManager.singleMatch(
            string = body,
            regex = "makePlay.+?return[^?]+([^\"]+)"
        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))

        val requesterUrl = "https://$host$keysCode"

        response = client.newCall(
            GET(
                url = requesterUrl,
                headers = hashMapOf(
                    "Referer" to referer
                )
            )
        ).await()

        body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))

        response = client.newCall(
            GET(
                headers = requestHeaders(response.request.headers, host)
            )
        ).await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = (body + getRandomBuilderString() + token + (System.currentTimeMillis() / 1000L)),
                headers = response
                    .request
                    .headers
                    .toMap()
            )
        )
    }

    private fun getRandomBuilderString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun requestHeaders(headers: Headers, host: String) : HashMap<String,String> {
        return HashMap(
            headers.newBuilder()
                .add("User-Agent", "MoonGetter")
                .add("Referer", "https://$host/")
                .build()
                .toMap()
        )
    }
}