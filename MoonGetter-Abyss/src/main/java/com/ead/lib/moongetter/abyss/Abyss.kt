@file:OptIn(ExperimentalEncodingApi::class)

package com.ead.lib.moongetter.abyss

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class Abyss(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val dataInBase64 = Base64.decode(
            PatternManager.singleMatch(
                string = response.body?.string() ?: throw InvalidServerException(context.getString(
                    R.string.server_response_went_wrong, name)),
                regex = """atob\("([^"]+)""""
            )?.removeSuffix("_") ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))
        ).toString(Charsets.UTF_8)

        //saveFile(decodeBase64(dataInBase64))

        return listOf(
            Video(
                quality = DEFAULT,
                url = dataInBase64.last().toString()
            )
        )
    }

    /*fun decodeBase64(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.DEFAULT)
    }

    fun saveFile(bytes: ByteArray) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDir, "video${System.currentTimeMillis()}.mp4")
        FileOutputStream(file).use { it.write(bytes) }
    }*/
}