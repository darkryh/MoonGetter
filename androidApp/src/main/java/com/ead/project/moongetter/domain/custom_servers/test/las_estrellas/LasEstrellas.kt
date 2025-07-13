package com.ead.project.moongetter.domain.custom_servers.test.las_estrellas

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class LasEstrellas(
    url :String,
    client : MoonClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(url,client,headers,configData) {

    private val regex = """"contentUrl"\s*:\s*"([^"]+)"""".toRegex()
    private val regexParams = """[?&]mcpid=([^&]+).*?&mcpOrigin=([^&]+)""".toRegex()
    private val regexStream = """#EXT-X-STREAM-INF:.*?RESOLUTION=(\d+x\d+).*?\n(https?://\S+)""".toRegex()

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = url
        it["Origin"] = url
    }

    override suspend fun onExtract(): List<Video> {
        var response = client
            .GET()

        if (!response.isSuccess) throw InvalidServerException(
            Resources.unsuccessfulResponse(name),
            Error.UNSUCCESSFUL_RESPONSE,
            response.statusCode
        )

        var responseBody = response.body.asString()

        client.GET(requestUrl = "https://auth.univision.com/verify-auth")

        url = regex.find(responseBody)?.groups?.get(1)?.value.toString()

        val data = regexParams.find(url)

        val mcpid = data?.groups?.get(1)?.value
        val mcpOrigin = data?.groups?.get(2)?.value

        response = client
            .GET(
                requestUrl = "https://auth.univision.com/api/v3/video-auth/url-signature-token-by-id",
                queryParams = mapOf(
                    "mcpid" to mcpid.toString(),
                    "mcpOrigin" to mcpOrigin.toString()
                )
            )

        responseBody = response.body.asString()

        return regexStream.findAll(responseBody)
            .map { matchResult ->
                val resolution = matchResult.groups[1]?.value.orEmpty()
                val url = matchResult.groups[2]?.value.orEmpty()
                Video(
                    quality = resolution,
                    url = url,
                )
            }.toList()
    }
}