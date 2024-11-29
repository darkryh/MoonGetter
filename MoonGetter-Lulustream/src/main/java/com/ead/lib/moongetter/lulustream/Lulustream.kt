package com.ead.lib.moongetter.lulustream

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

@ExperimentalServer
class Lulustream(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = url
        it["Origin"] = url
    }

    override suspend fun onExtract() : List<Video> {
        val response = client
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        val body = response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = PatternManager.singleMatch(
                        string = body,
                        regex = """sources:\s*\[\s*\{file:\s*"(https?://[^"]+)"""
                    )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                    method = "GET",
                    headers = headers
                )
            )
        )
    }
}