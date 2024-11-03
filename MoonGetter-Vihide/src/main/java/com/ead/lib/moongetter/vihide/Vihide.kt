package com.ead.lib.moongetter.vihide

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.OkHttpClient

@ExperimentalServer
class Vihide(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Origin"] = url
        it["Referer"] = url
    }

    override suspend fun onExtract(): List<Video> {
        var response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = JsUnpacker.unpackAndCombine(
                        response.body?.string() ?: throw InvalidServerException(context.getString(
                            R.string.server_response_went_wrong, name)
                        )
                    ) ?: throw InvalidServerException(context.getString(R.string.server_response_packed_function_not_found, name)),
                    regex = """file\s*:\s*"(https?://[^"]+\.m3u8\?[^"]*)""".trimIndent()
                ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))
            )
        )
    }
}