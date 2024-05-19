package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class GoogleDrive(context: Context, url : String) : Server(context,url) {

    override var url: String = fixUrl(url)

    override suspend fun onExtract() {
        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        when (response.code) {
            206 -> addDefault()
            200 -> {
                val body = response.body?.string() ?: return

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

                url = generateDownloadUrl(
                    id = id ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier)),
                    export = export ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier)),
                    confirm = confirm ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier)),
                    uuid = uuid ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.GoogleDriveIdentifier))
                )

                addDefault()
            }
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