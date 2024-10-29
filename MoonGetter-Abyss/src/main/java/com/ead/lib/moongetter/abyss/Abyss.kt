package com.ead.lib.moongetter.abyss

import android.content.Context
import android.os.Environment
import android.util.Base64
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.decoder
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream

class Abyss(
    context: Context,
    url : String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val dataInBase64 = decoder(
            PatternManager.singleMatch(
                string = response.body?.string() ?: throw InvalidServerException(context.getString(
                    R.string.server_response_went_wrong, name)),
                regex = """atob\("([^"]+)""""
            )?.removeSuffix("_") ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))
        )

        saveFile(decodeBase64(dataInBase64))

        return listOf(
            Video(
                quality = DEFAULT,
                url = dataInBase64.last().toString()
            )
        )
    }

    fun decodeBase64(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.DEFAULT)
    }

    fun saveFile(bytes: ByteArray) {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadDir, "video${System.currentTimeMillis()}.mp4")
        FileOutputStream(file).use { it.write(bytes) }
    }
}