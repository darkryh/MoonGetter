package com.ead.lib.moongetter.okru

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONObject

class Okru(
    context: Context,
    url : String,
    headers : HashMap<String, String>
) : Server(context, url, headers) {

    override val headers: HashMap<String, String> = headers.also { values -> values["User-Agent"] = USER_AGENT }

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name))

        url = PatternManager.singleMatch(
            string =  response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            regex =  "data-options=\"(.*?)\""
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,name))

        url = StringEscapeUtils.unescapeHtml4(url)

        val json = JSONObject(url)
            .getJSONObject("flashvars")
            .getString("metadata")

        val objectData = JSONObject(json).getJSONArray("videos")

        return (0 .. objectData.length() -1).map { pos ->
            val url: String = objectData.getJSONObject(pos).getString("url")
            when (objectData.getJSONObject(pos).getString("name")) {
                "mobile" -> Video("144p", url)
                "lowest" -> Video("240p", url)
                "low"    -> Video("360p", url)
                "sd"     -> Video("480p", url)
                "hd"     -> Video("720p", url)
                "full"   -> Video("1080p", url)
                "quad"   -> Video("2000p", url)
                "ultra"  -> Video("4000p", url)
                else     -> Video("Default", url)
            }.let { video ->
                video.copy(
                    request = video.request.copy(
                        headers = headers
                    )
                )
            }
        }
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19"
    }
}