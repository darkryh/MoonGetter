package com.ead.lib.moongetter.vihide

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.OkHttpClient

@ExperimentalServer
class Vihide(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Origin"] = url
        it["Referer"] = url
    }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string = JsUnpacker.unpackAndCombine(
                        response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)
                    ) ?: throw InvalidServerException(Resources.expectedPackedResponseNotFound(name), Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND),
                    regex = """file\s*:\s*"(https?://[^"]+\.m3u8\?[^"]*)""".trimIndent()
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
            )
        )
    }
}