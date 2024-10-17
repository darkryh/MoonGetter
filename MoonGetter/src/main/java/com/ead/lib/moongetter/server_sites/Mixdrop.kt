package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.JSUnpacker
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

@Pending
class Mixdrop(context: Context, url: String) : Server(context, url) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(
                Request
                    .Builder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
                    .url(url)
                    .build()
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, Properties.MixdropIdentifier))

        val responseBody = response.body?.string().toString()

        if (!JSUnpacker.detect(responseBody)) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, Properties.MixdropIdentifier))

        return listOf(
            Video(
                quality = DEFAULT,
                url = "https:" + PatternManager.singleMatch(
                    string = JSUnpacker.unpack(
                        PatternManager.singleMatch(
                            string = responseBody.toString(),
                            regex = "eval(.*)"
                        ).toString()
                    ).toString(),
                    regex = "wurl=?\"(.*?)\";"
                ).toString()
            )
        )
    }
}