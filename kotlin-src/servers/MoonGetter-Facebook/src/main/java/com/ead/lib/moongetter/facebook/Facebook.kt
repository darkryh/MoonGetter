@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.facebook

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

class Facebook(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    init {
        targetUrl = targetUrl ?: "https://fdown.net/download.php"
    }

    override suspend fun onExtract() : List<Video> {
        val response = client
            .POST(
                requestUrl = targetUrl,
                body = FDownBody(url),
                asFormUrlEncoded = true
            )

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        val body = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

        return listOf(
            Video(
                quality = DEFAULT,
                url = (PatternManager.singleMatch(
                    string = body,
                    regex = "id=\"sdlink\".*?href=\"(.*?)\""
                ) ?: PatternManager.singleMatch(
                    string = body,
                    regex = "id=\"hdlink\".*?href=\"(.*?)\""
                ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)).replace("&amp;", "&")
            )
        )
    }
}

@Serializable
data class FDownBody(
    @SerialName("URLz") val url : String
)