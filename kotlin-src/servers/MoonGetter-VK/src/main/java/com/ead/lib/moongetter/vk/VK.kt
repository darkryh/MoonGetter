package com.ead.lib.moongetter.vk

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager

class VK(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {
    companion object {
        private const val VK_URL = "https://vk.com"
    }

    override val headers: HashMap<String, String> = headers.also {
        it["Accept"] = "*/*"
        it["Origin"] = VK_URL
        it["Referer"] = "$VK_URL/"
    }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET(
                overrideHeaders = mapOf(
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                )
            )

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        val responseBody = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

        return PatternManager.findMultipleMatchesAsPairs(
            string = responseBody,
            regex = """"url(\d+)":"(.*?)"""".trimIndent(),
        ).ifEmpty { throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND) }
            .map {
                Video(
                    quality = it.first,
                    url = it.second,
                    headers = headers
                )
            }
    }
}