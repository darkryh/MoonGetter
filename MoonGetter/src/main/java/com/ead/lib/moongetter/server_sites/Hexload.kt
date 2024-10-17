package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class Hexload(context: Context,url: String) : Server(context,url) {

    override suspend fun onExtract(): List<Video> {
        val domain = PatternManager.singleMatch(
            string = url,
            regex = """https?://([a-zA-Z0-9\-]+\.[a-z]{2,})/embed-[^/]+\.html"""
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.HexloadIdentifier))

        url = url.replace(domain, "hexload.com")

        var response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.HexloadIdentifier))


        val dataPattern = """data:\s*\{\s*(.*?)\s*\}""".toRegex(RegexOption.DOT_MATCHES_ALL)
        val dataContent = dataPattern.find(response.body?.string().toString())?.groupValues?.get(1)

        val dataParams = PatternManager.findMultipleMatchesAsPairs(
            string = dataContent.toString(),
            regex = """(\w+):\s*['"]([^'"]+)['"]"""
        )

        val formBodyBuilder = FormBody.Builder()

        dataParams.forEach { (key, value) -> formBodyBuilder.add(key, value) }

        val client = OkHttpClient()
        val formBody = formBodyBuilder.build()

        val request = Request.Builder()
            .url("https://hexload.com/download")
            .post(formBody)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        response = client.newCall(request).await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.HexloadIdentifier))

        val responseBody = response.body?.string().toString()

        val jsonObject = JSONObject(responseBody)

        return listOf(
            Video(
                quality = DEFAULT,
                url = jsonObject.getJSONObject("result").getString("url")
            )
        )
    }
}