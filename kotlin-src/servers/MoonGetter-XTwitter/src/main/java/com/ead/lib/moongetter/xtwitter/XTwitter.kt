@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.xtwitter

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class XTwitter(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    init {
        targetUrl = targetUrl ?: "https://twdown.net/download.php"
    }

    override suspend fun onExtract(): List<Video> {
        val response = client
            .POST(
                requestUrl = targetUrl,
                body = XTwitterBody(url),
                asFormUrlEncoded = true
            )

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        return PatternManager.findMultipleMatches(
            string = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
            regex = """<td>.*?<a\s+[^>]*?href=['"]([^'"]+)['"].*?>.*?</a>.*?</td>"""
        )
            .filter { it != "#" }
            .ifEmpty { throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND) }
            .map { url ->
                Video(
                    quality = PatternManager.singleMatch(
                        string = url,
                        regex = """/(\d{3,4})x(\d{3,4})/"""
                    ) ?: if (url.contains("mp3.php?")) {
                        "mp3"
                    }
                    else DEFAULT,
                    url = url
                )
            }
    }
}


@Serializable
data class XTwitterBody(
    @SerialName("URL") val url : String
)