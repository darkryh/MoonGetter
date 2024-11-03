package com.ead.project.moongetter.domain.custom_servers

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class SenvidModified(
    context : Context,
    url :String,
    client : OkHttpClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(context,url,client,headers,configData) {

    private val urlRegex =  """https://custom\.domain\.com/aqua/sv\?url=([^&]+)""".toRegex()

    override var url: String = urlRegex.find(url)?.groupValues?.get(1) ?:
    throw InvalidServerException(context.getString(R.string.unknown_error))

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(
            R.string.server_domain_is_down,
            name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string =  response.body?.string().toString(),
                    regex =  "<source src=\"(.*?)\""
                ) ?: throw InvalidServerException(context.getString(
                    R.string.server_requested_resource_was_taken_down,
                    name)
                )
            )
        )
    }
}