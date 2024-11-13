package com.ead.lib.moongetter.filemoon

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.OkHttpClient

@ExperimentalServer
class Filemoon(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        it["Accept-Language"] = "en-US,en;q=0.5"
        it["Priority"] = "u=0, i"
        it["Origin"] = url
        it["Referer"] = url
    }

    override suspend fun onExtract(): List<Video> {
        var response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name))

        url = PatternManager.singleMatch(
            string =  response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            regex = """<iframe\s+[^>]*src=["'](https?://[^"']+)["'][^>]*>"""
        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))

        response = client
            .configBuilder()
            .newCall(
                GET(
                    overrideHeaders = response.headers.let {
                        val builder = it.newBuilder()

                        headers.forEach { (key, value) ->
                            builder.add(key, value)
                        }

                        builder.build()
                    }
                )
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = PatternManager.singleMatch(
                        string = JsUnpacker.unpackAndCombine(
                            response.body?.string() ?: throw InvalidServerException(context.getString(
                                R.string.server_response_went_wrong, name)
                            )
                        ) ?: throw InvalidServerException(context.getString(R.string.server_response_packed_function_not_found, name)),
                        regex = """file\s*:\s*"(https?://[^"]+\.m3u8\?[^"]*)""".trimIndent()
                    ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name)),
                    headers = headers,
                    method = "GET"
                )
            )
        )
    }
}