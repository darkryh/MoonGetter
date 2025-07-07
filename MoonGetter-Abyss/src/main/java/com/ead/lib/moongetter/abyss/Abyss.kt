package com.ead.lib.moongetter.abyss

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.core.Resources
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager

class Abyss(
    url : String,
    client: MoonClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    override val isDeprecated: Boolean = true

    override suspend fun onExtract(): List<Video> {
        val response = client.GET()

        if (!response.isSuccess) throw InvalidServerException(Resources.unsuccessfulResponse(name), Error.UNSUCCESSFUL_RESPONSE, response.statusCode)

        val dataInBase64 = decoder(
            PatternManager.singleMatch(
                string = response.body.asString().ifEmpty { throw InvalidServerException(Resources.emptyOrNullResponse(name), Error.EMPTY_OR_NULL_RESPONSE) },
                regex = """atob\("([^"]+)""""
            )?.removeSuffix("_") ?: throw InvalidServerException(Resources.expectedResponseNotFound(name), Error.EXPECTED_RESPONSE_NOT_FOUND)
        )

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

fun decoder(encodedString: String): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
    val output = StringBuilder()

    var i = 0
    while (i < encodedString.length) {
        val index1 = chars.indexOf(encodedString[i++])
        val index2 = chars.indexOf(encodedString[i++])
        val index3 = chars.indexOf(encodedString[i++])
        val index4 = chars.indexOf(encodedString[i++])

        val bits = (index1 shl 18) or (index2 shl 12) or (index3 shl 6) or index4

        output.append(((bits shr 16) and 0xff).toChar())
        if (index3 != 64) {
            output.append(((bits shr 8) and 0xff).toChar())
        }
        if (index4 != 64) {
            output.append((bits and 0xff).toChar())
        }
    }

    return output.toString()
}