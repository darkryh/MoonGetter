package com.ead.lib.moongetter.hexload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import org.json.JSONObject

class Hexload(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : Server(context,url,headers) {

    override suspend fun onExtract(): List<Video> {
        val domain = PatternManager.singleMatch(
            string = url,
            regex = """https?://([a-zA-Z0-9\-]+\.[a-z]{2,})/embed-[^/]+\.html"""
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        url = url.replace(domain, "hexload.com")

        var response = OkHttpClient()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val dataPattern = """data:\s*\{\s*(.*?)\s*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val dataContent = dataPattern.find((response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))))?.groupValues?.get(1)

        response = OkHttpClient()
            .newCall(
                POST(
                    url = url,
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

        val jsonObject = JSONObject(responseBody)

        return listOf(
            Video(
                quality = DEFAULT,
                url = jsonObject.getJSONObject("result").getString("url")
            )
        )
    }
}