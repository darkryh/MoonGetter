package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.text.StringEscapeUtils
import org.json.JSONObject

class Okru(context: Context, url : String) : Server(context,url) {

    override var url: String = url.replace("http://","https://")

    override suspend fun onExtract() {

        val response = OkHttpClient()
            .newCall(
                Request.Builder().url(url)
                    .header("User-Agent", Properties.okruUserAgent).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.OkruIdentifier))

        url = PatternManager.singleMatch(
            string =  response.body?.string().toString(),
            regex =  "data-options=\"(.*?)\""
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.OkruIdentifier))

        url = StringEscapeUtils.unescapeHtml4(url)

        val json = JSONObject(url)
            .getJSONObject("flashvars")
            .getString("metadata")

        val objectData = JSONObject(json).getJSONArray("videos")
        var video: Video

        for (pos in 0 until objectData.length()) {
            val url: String = objectData.getJSONObject(pos).getString("url")
            video = when (objectData.getJSONObject(pos).getString("name")) {
                "mobile" -> Video("144p", url)
                "lowest" -> Video("240p", url)
                "low"    -> Video("360p", url)
                "sd"     -> Video("480p", url)
                "hd"     -> Video("720p", url)
                "full"   -> Video("1080p", url)
                "quad"   -> Video("2000p", url)
                "ultra"  -> Video("4000p", url)
                else     -> Video("Default", url)
            }
            add(video)
        }
    }
}