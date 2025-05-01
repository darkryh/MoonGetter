package com.ead.project.moongetter.domain.custom_servers.test.las_estrellas

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okhttp3.OkHttpClient

class LasEstrellas(
    url :String,
    client : OkHttpClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(url,client,headers,configData) {

    val regex = """"contentUrl"\s*:\s*"([^"]+)"""".toRegex()
    val regexParams = """[?&]mcpid=([^&]+).*?[&]mcpOrigin=([^&]+)""".toRegex()
    val regexStream = """#EXT-X-STREAM-INF:.*?RESOLUTION=(\d+x\d+).*?\n(https?://[^\s]+)""".toRegex()

    override val headers: HashMap<String, String> = headers.also {
        it["Referer"] = url
        it["Origin"] = url
    }

    override suspend fun onExtract(): List<Video> {
        var response = client
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(
            Resources.unsuccessfulResponse(name),
            Error.UNSUCCESSFUL_RESPONSE,
            response.code
        )

        var responseBody = response.body?.string().toString()

        response = client
            .newCall(
                GET(url = "https://auth.univision.com/verify-auth")
            )
            .await()

        url = regex.find(responseBody)?.groups?.get(1)?.value.toString()

        val data = regexParams.find(url)

        val mcpid = data?.groups?.get(1)?.value
        val mcpOrigin = data?.groups?.get(2)?.value

        response = client
            .newCall(
                GET(
                    url = "https://auth.univision.com/api/v3/video-auth/url-signature-token-by-id",
                    queryParameters = mapOf(
                        "mcpid" to mcpid.toString(),
                        "mcpOrigin" to mcpOrigin.toString()
                    )
                )
            )
            .await()

        responseBody = response.body?.string().toString()

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