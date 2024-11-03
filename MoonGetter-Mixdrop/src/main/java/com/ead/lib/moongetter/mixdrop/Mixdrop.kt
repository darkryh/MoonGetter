package com.ead.lib.moongetter.mixdrop

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.OkHttpClient

@ExperimentalServer
class Mixdrop(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = DEFAULT_REFERER
        it["Origin"] = DEFAULT_REFERER
    }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))

        if (!JsUnpacker.detect(body)) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = "https:" + (
                            PatternManager.singleMatch(
                                string = JsUnpacker.unpackAndCombine(
                                    PatternManager.singleMatch(
                                        string = body,
                                        regex = "eval(.*)",
                                        groupIndex = 0
                                    ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))
                                ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name)),
                                regex = """wurl="?\"(.*?)\";""".trimIndent()
                            ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))
                            ),
                    headers = headers,
                    method = "GET"
                )
            )
        )
    }

    companion object {
        private const val DEFAULT_REFERER = "https://mixdrop.co/"
    }
}