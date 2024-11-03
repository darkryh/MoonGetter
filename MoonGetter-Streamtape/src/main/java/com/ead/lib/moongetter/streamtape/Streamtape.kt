package com.ead.lib.moongetter.streamtape

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class Streamtape(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name))

        val body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong,name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = "https:" +
                        (PatternManager.singleMatch(
                            string = body,
                            regex = """div id="robotlink" style="display:none;">(.*?token=)[^&]*""".trimIndent()
                        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))) +
                        (PatternManager.singleMatch(
                            string = body,
                            regex = """document\.getElementById\('robotlink'\)\.innerHTML\s*=\s*.*?token=([\w-]+)""".trimIndent()
                        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name)))
            )
        )
    }

}