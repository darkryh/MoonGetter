package com.ead.lib.moongetter.yourupload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class YourUpload(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also { it.remove("User-Agent") }

    override suspend fun onExtract(): List<Video> {
        var response = client
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string =  response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
                    regex = """file:\s*'([^']*)'""",
                ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name)),
                headers = hashMapOf(
                    "Referer" to "https://www.yourupload.com/"
                )
            )
        )
    }
}