package com.ead.lib.moongetter.okru

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.JsonObject
import com.ead.lib.moongetter.utils.PatternManager
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.apache.commons.text.StringEscapeUtils

class Okru(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val headers: HashMap<String, String> = headers.also { values -> values["User-Agent"] = USER_AGENT }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        url = PatternManager.singleMatch(
            string = response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
            regex = "data-options=\"(.*?)\""
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        url = StringEscapeUtils.unescapeHtml4(url)

        val metadata = JsonObject.fromJson(url)
            .getJSONObject("flashvars")
            ?.getString("metadata")
            ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        val objectData = JsonObject.fromJson(metadata).getJSONArray("videos") ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        return objectData.indices.map { pos ->
            /**
             * Get the video object
             */
            val videoJson = objectData[pos]

            /**
             * Get the url
             */
            val url: String = videoJson.getString("url") ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

            /**
             * Get the quality and return the video object
             */
            when (videoJson.getString("name")) {
                "mobile" -> Video("144p", url)
                "lowest" -> Video("240p", url)
                "low" -> Video("360p", url)
                "sd" -> Video("480p", url)
                "hd" -> Video("720p", url)
                "full" -> Video("1080p", url)
                "quad" -> Video("2000p", url)
                "ultra" -> Video("4000p", url)
                else -> Video("Default", url)
            }
        }
    }

    companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19"
    }
}