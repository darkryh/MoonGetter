package com.ead.lib.moongetter.xtwitter

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.FormBody
import okhttp3.OkHttpClient
import kotlin.collections.ifEmpty
import kotlin.collections.map

class XTwitter(
    context: Context,
    url: String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {

    var targetUrl = "https://twdown.net/download.php"

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .configBuilder()
            .newCall(
                POST(
                    url = targetUrl,
                    formBody = FormBody.Builder()
                        .add("URL", url)
                        .build(),
                )
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return PatternManager.findMultipleMatches(
            string = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            regex = """<td>.*?<a\s+[^>]*?href=['"]([^'"]+)['"].*?>.*?</a>.*?</td>"""
        )
            .filter { it != "#" }
            .ifEmpty { throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name)) }
            .map { url ->
                Video(
                    quality = PatternManager.singleMatch(
                        string = url,
                        regex = """/(\d{3,4})x(\d{3,4})/"""
                    ) ?: if (url.contains("mp3.php?")) {
                        "mp3"
                    }
                    else DEFAULT,
                    url = url
                )
            }
    }
}