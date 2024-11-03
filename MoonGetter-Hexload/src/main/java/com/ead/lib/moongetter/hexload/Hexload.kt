@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.hexload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.core.system.extensions.replaceDomainWith
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.JsonObject
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import com.ead.lib.moongetter.utils.Values.targetUrl2
import okhttp3.FormBody
import okhttp3.OkHttpClient

class Hexload(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override var url: String = targetUrl ?: url.replaceDomainWith("hexload.com")
    ?: throw InvalidServerException(context.getString(R.string.url_provided_is_not_expected, name))

    override suspend fun onExtract(): List<Video> {
        var response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val dataPattern = """data:\s*\{\s*(.*?)\s*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val dataContent = dataPattern.find((response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))))?.groupValues?.get(1)

        response = client
            .configBuilder()
            .newCall(
                POST(
                    url = targetUrl2,
                    headers = hashMapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    formBody = PatternManager.findMultipleMatchesAsPairs(
                        string = dataContent.toString(),
                        regex = """(\w+):\s*['"]([^'"]+)['"]"""
                    ).ifEmpty { throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name)) }
                        .let { bodyParams ->
                            val formBody = FormBody.Builder()

                            bodyParams.forEach { (key, value) ->  formBody.add(key, value) }

                            formBody.build()
                        }
                )
            )
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val responseBody = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = JsonObject
                    .fromJson(responseBody)
                    .getJSONObject("result")
                    ?.getString("url")
                    ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            )
        )
    }
}