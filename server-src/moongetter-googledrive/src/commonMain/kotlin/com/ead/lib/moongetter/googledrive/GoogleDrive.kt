@file:Suppress("RestrictedApi","VisibleForTests")

package com.ead.lib.moongetter.googledrive

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.core.system.extensions.extractFirst
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.ExtractionStrategy

class GoogleDrive(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override var url: String = com.ead.lib.moongetter.utils.Values.targetUrl
        ?: "https://drive.usercontent.google.com/download?id=${getFileId(url)}&export=download"

    override suspend fun onExtract(): List<Video> {
        val response = ExtractionStrategy.withRetry {
            client.GET()
        }

        return when (response.statusCode) {
            206 -> listOf(
                Video(
                    url = url
                )
            )
            200 -> {
                val body = response.body.asString().ifEmpty {
                    throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE)
                }

                val id = body.extractFirst(getRegexProperty("id"))
                    ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

                val export = body.extractFirst(getRegexProperty("export"))
                    ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

                val confirm = body.extractFirst(getRegexProperty("confirm"))
                    ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

                val uuid = body.extractFirst(getRegexProperty("uuid"))
                    ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)

                listOf(
                    Video(
                        url = generateDownloadUrl(
                            id = id,
                            export = export,
                            confirm = confirm,
                            uuid = uuid
                        )
                    )
                )
            }
            else -> throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)
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
        return string.extractFirst("""\/file\/d\/([^\/?&]+)""")
            ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
    }

    private fun getRegexProperty(name : String) : String {
        return "(?<=name=\"$name\" value=\")(.*?)(?=\")"
    }
}