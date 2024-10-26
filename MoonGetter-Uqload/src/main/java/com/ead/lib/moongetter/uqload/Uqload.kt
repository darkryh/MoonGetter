package com.ead.lib.moongetter.uqload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

@Pending
@Unstable("There is some validations probably in the player side to accept the request")
class Uqload(
    context: Context,
    url: String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract() : List<Video> {
        val response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))


        val headers = mapOf(
            "Referer" to "https://uqload.ws/",
        )

        url = PatternManager.singleMatch(
            string = body,
            regex = "sources:\\s*\\[\"(http[^\\\"]+)"
        )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = url,
                headers = headers
            )
        )
    }
}