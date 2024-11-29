@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.mp4upload

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.ServerUCR
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import okhttp3.OkHttpClient

@ExperimentalServer
class Mp4Upload(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerUCR(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = url
        it["Origin"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract() : List<Video> {
        var response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = PatternManager.singleMatch(
                        string = response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE),
                        regex = """src:\s*"([^"]+\.mp4)""""
                    )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                    method = "GET",
                    headers = headers
                )
            )
        )
    }
}