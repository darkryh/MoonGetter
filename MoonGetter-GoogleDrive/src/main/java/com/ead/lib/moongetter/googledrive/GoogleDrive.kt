@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.googledrive

import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.Values.targetUrl
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText

class GoogleDrive(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override var url: String = targetUrl ?: "https://drive.usercontent.google.com/download?id=${getFileId(url)}&export=download"

    override suspend fun onExtract(): List<Video> {
        val response = client
            .GET()

        return when (response.status.value) {
            206 -> listOf(
                Video(
                    quality = DEFAULT,
                    url = url
                )
            )
            200 -> {
                val body = response.bodyAsText().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) }

                val id = PatternManager.singleMatch(
                    string = body,
                    regex = getRegexProperty("id")
                )

                val export = PatternManager.singleMatch(
                    string = body,
                    regex = getRegexProperty("export")
                )

                val confirm = PatternManager.singleMatch(
                    string = body,
                    regex = getRegexProperty("confirm")
                )

                val uuid = PatternManager.singleMatch(
                    string = body,
                    regex = getRegexProperty("uuid")
                )

                listOf(
                    Video(
                        quality = DEFAULT,
                        url = generateDownloadUrl(
                            id = id ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                            export = export ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                            confirm = confirm ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND),
                            uuid = uuid ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
                        )
                    )
                )
            }
            else -> throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.status.value)
        }
    }

    private fun generateDownloadUrl(
        id : String,
        export : String,
        confirm : String,
        uuid : String
    ) : String {
        return "https://drive.usercontent.google.com/download?id=$id&export=$export&authuser=0&confirm=$confirm&uuid=$uuid"
    }

    private fun getFileId(string: String) : String {
        return PatternManager.singleMatch(
            string = string,
            regex = """\/file\/d\/([^\/?&]+)"""
        ) ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
    }

    private fun getRegexProperty(name : String) : String {
        return "(?<=name=\"$name\" value=\")(.*?)(?=\")"
    }
}