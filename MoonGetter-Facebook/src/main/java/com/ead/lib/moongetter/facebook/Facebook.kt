package com.ead.lib.moongetter.facebook

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.FormBody
import okhttp3.OkHttpClient

class Facebook(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : Server(context,url,headers) {

    var targetUrl = "https://fdown.net/download.php"

    override suspend fun onExtract() : List<Video> {
        val response = OkHttpClient()
            .newCall(
                POST(
                    url = targetUrl,
                    formBody = FormBody.Builder()
                        .add("URLz", url)
                        .build()
                )
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = (PatternManager.singleMatch(
                    string = body,
                    regex = "id=\"sdlink\".*?href=\"(.*?)\""
                ) ?: PatternManager.singleMatch(
                    string = body,
                    regex = "id=\"hdlink\".*?href=\"(.*?)\""
                ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))).replace("&amp;", "&")
            )
        )
    }
}