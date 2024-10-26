package com.ead.lib.moongetter.voe

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.decoder
import okhttp3.OkHttpClient

class Voe(
    context: Context,
    url : String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {

    override suspend fun onExtract(): List<Video> {
        var response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        url = PatternManager.singleMatch(
            string = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            regex = """window\.location\.href\s*=\s*'([^']+)"""
        ).toString()

        response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = decoder(
                    PatternManager.singleMatch(
                        string = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
                        regex = """'hls':\s*'([^']+)"""
                    ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))
                )
            )
        )
    }
}