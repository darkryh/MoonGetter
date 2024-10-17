package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

@Unstable(reason = "Needs to adapt code to multiple downloads link in Drive")
class GoogleDrive(context: Context, url : String) : Server(context,url) {

    override var url: String = fixUrl(url)

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        return when (response.code) {
            206 -> listOf(
                Video(
                    quality = DEFAULT,
                    url = url
                )
            )
            200 -> {
                val body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down))

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
                            id = id ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier)),
                            export = export ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier)),
                            confirm = confirm ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier)),
                            uuid = uuid ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier))
                        )
                    )
                )
            }
            else -> throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.GoogleDriveIdentifier))
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
    private fun fixUrl(url : String) = "https://drive.usercontent.google.com/download?id=${getFileId(url)}&export=download"

    private fun getFileId(string: String) : String {
        return PatternManager.singleMatch(
            string = string,
            regex = """\/file\/d\/([^\/?&]+)"""
        ) ?: throw InvalidServerException(context.getString(R.string.server_regex_could_not_find_id,Properties.GoogleDriveIdentifier))
    }

    private fun getRegexProperty(name : String) : String {
        return "(?<=name=\"$name\" value=\")(.*?)(?=\")"
    }
}