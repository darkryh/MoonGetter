package com.ead.lib.moongetter.yourupload

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class YourUpload(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override suspend fun onExtract(): List<Video> {
        val response = client
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string =  response.body?.string() ?: throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE),
                    regex = """file:\s*'([^']*)'""",
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                headers = hashMapOf(
                    "Referer" to "https://www.yourupload.com/"
                )
            )
        )
    }
}