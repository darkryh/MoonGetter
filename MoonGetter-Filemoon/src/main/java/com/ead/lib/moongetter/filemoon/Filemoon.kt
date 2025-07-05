@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.filemoon

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.PlaylistUtils
import com.ead.lib.moongetter.utils.Values.targetUrl
import com.ead.lib.moongetter.utils.toHashMap
import com.ead.lib.moongetter.utils.toHeaders
import dev.datlag.jsunpacker.JsUnpacker
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

@ExperimentalServer
class Filemoon(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    private val playlistUtils by lazy { PlaylistUtils(client, headers.toHeaders()) }

    override val headers: HashMap<String, String> = headers.also {
        it["Accept"] = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        it["Accept-Language"] = "en-US,en;q=0.5"
        it["Priority"] = "u=0, i"
        it["Origin"] = url
        it["Referer"] = url
    }

    override var url: String = targetUrl ?: url

    override suspend fun onExtract(): List<Video> {
        var response = client
            .GET()

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        url = PatternManager.singleMatch(
            string =  response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
            regex = """<iframe\s+[^>]*src=["'](https?://[^"']+)["'][^>]*>"""
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        response = client
            .GET(
                overrideHeaders = response
                    .headers
                    .toHashMap()
            )

        if (!response.status.isSuccess()) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)

        val playlistUrl = PatternManager.singleMatch(
            string = JsUnpacker.unpackAndCombine(
                response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }
            ) ?: throw InvalidServerException(Resources.expectedPackedResponseNotFound(name), Error.EXPECTED_PACKED_RESPONSE_NOT_FOUND),
            regex = """(https://[^\s"']+\.m3u8(?:\?[^\s"']*)?)""".trimIndent()
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

        return playlistUtils.extractFromHls(playlistUrl)
    }
}