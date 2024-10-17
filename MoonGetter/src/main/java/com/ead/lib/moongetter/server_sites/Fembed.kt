package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.core.system.extensions.delete
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class Fembed(context: Context, url : String) : Server(context,url) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract(): List<Video> {
        var request: Request =  Request.Builder().url(url).build()

        var response = OkHttpClient()
            .newCall(request)
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.FembedIdentifier))

        val host = response.request.url.host

        val matchVideoId = PatternManager.singleMatch(
            string = url,
            regex = "([vf])([/=])(.+)([/&])?",
            groupIndex = 3
        ) ?: throw InvalidServerException(context.getString(R.string.server_regex_could_not_find_id,Properties.FembedIdentifier))

        val videoId = matchVideoId
            .delete("[&/]")

        request = Request.Builder().url(getApiUrl(host = host, id = videoId))
            .post(FormBody.Builder().build())
            .build()

        response = OkHttpClient().newCall(request).await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.FembedIdentifier))

        val body = response.body?.string().toString()
        val source = JSONObject(body)
        check(source.getBoolean("success")) {  "Request was not succeeded" }

        val array = source.getJSONArray("data")

        return (0 .. array.length()).map {
            val `object` = array.getJSONObject(it)
            val name = `object`.getString("label")
            url = `object`.getString("file")
            Video(name, url)
        }
    }

    private fun getApiUrl(host : String, id : String) : String = "https://$host/api/source/$id"

}