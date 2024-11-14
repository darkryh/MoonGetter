package com.ead.lib.moongetter.streamwish

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.OkHttpClient

class Streamwish(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it.remove("User-Agent")
        it["Origin"] = url
        it["Referer"] = url
    }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = JsUnpacker.unpackAndCombine(
                        response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name), Error.EMPTY_OR_NULL_RESPONSE)
                    ) ?: throw InvalidServerException(context.getString(R.string.server_response_packed_function_not_found, name), Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND),
                    regex = """file\s*:\s*"(https?://[^"]+\.m3u8\?[^"]*)""".trimIndent()
                ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name), Error.EXPECTED_RESPONSE_NOT_FOUND)
            )
        )
    }
}