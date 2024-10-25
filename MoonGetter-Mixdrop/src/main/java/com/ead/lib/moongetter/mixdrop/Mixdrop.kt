package com.ead.lib.moongetter.mixdrop

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.JSUnpacker
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class Mixdrop(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : Server(context,url,headers) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = DEFAULT_REFERER
        it["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
    }

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val responseBody = response.body?.toString() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))

        if (!JSUnpacker.detect(responseBody)) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

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
                ).toString(),
                headers = headers.toMap()
            )
        )
    }

    companion object {
        private const val DEFAULT_REFERER = "https://mixdrop.co/"
    }
}