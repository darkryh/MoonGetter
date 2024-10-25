package com.ead.lib.moongetter.goodstream

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.download.Request
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class GoodStream(
    context: Context,
    url : String,
    headers : HashMap<String,String>
) : Server(context,url,headers) {

    override val isDeprecated: Boolean = false

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = PatternManager.singleMatch(
                        string = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
                        regex = """file:\s*"(https?://[^\s"]+)""""
                    ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name)),
                    method = "GET",
                    headers = mapOf(
                        "User-Agent" to headers["User-Agent"].toString(),
                        "Referer" to "https://goodstream.one",
                        "Origin" to "https://goodstream.one"
                    )
                )
            )
        )
    }
}