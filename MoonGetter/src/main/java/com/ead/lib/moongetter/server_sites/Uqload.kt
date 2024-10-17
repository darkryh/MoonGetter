package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

@Pending
@Unstable("There is some validations probably in the player side to accept the request")
class Uqload(context: Context, url: String) : Server(context, url) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract() : List<Video> {
        val response = OkHttpClient()
            .newCall(
                Request
                    .Builder()
                    .url(url)
                    .build()
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException("Unexpected code $response")

        val body = response.body?.string() ?: throw InvalidServerException("Unexpected code body")


        val headers = mapOf(
            "Referer" to "https://uqload.ws/",
        )

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = body,
                    regex = "sources:\\s*\\[\"(http[^\\\"]+)"
                )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, Properties.UqloadIdentifier)),
                headers = headers
            )
        )
    }
}