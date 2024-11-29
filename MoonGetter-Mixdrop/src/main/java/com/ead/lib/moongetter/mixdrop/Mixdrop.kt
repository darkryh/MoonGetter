package com.ead.lib.moongetter.mixdrop

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import dev.datlag.jsunpacker.JsUnpacker
import okhttp3.OkHttpClient

@ExperimentalServer
class Mixdrop(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = DEFAULT_REFERER
        it["Origin"] = DEFAULT_REFERER
    }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        val body = response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)

        if (!JsUnpacker.detect(body)) throw InvalidServerException(Resources.expectedPackedResponseNotFound(name), Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND)

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
                                    ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
                                ) ?: throw InvalidServerException(Resources.expectedPackedResponseNotFound(name), Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND),
                                regex = """wurl="?\"(.*?)\";""".trimIndent()
                            ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
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