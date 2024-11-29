package com.ead.project.moongetter.domain.custom_servers

import android.content.Context
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

class SenvidModified(
    url :String,
    client : OkHttpClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : Server(url,client,headers,configData) {

    /*private val urlRegex =  """https://custom\.domain\.com/aqua/sv\?url=([^&]+)""".toRegex()

    override var url: String = urlRegex.find(url)?.groupValues?.get(1) ?:
    throw InvalidServerException(context.getString(R.string.url_provided_is_not_expected), Error.INVALID_PROCESS_IN_EXPECTED_URL_ENTRY)
*/
    override suspend fun onExtract(): List<Video> {
        /*val response = client
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(
            R.string.server_domain_is_down,
            name), Error.UNSUCCESSFUL_RESPONSE, response.code)

        return listOf(
            Video(
                quality = DEFAULT,
                url = PatternManager.singleMatch(
                    string =  response.body?.string().toString(),
                    regex =  "<source src=\"(.*?)\""
                ) ?: throw InvalidServerException(context.getString(
                    R.string.server_requested_resource_was_taken_down,
                    name), Error.EXPECTED_RESPONSE_NOT_FOUND)
            )
        )*/
        return emptyList()
    }
}