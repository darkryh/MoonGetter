package com.ead.lib.moongetter.yourupload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient

@Unstable("server request take too long to respond")
class YourUpload(
    context: Context,
    url : String,
    headers : HashMap<String,String>
) : Server(context,url,headers) {

    override suspend fun onExtract(): List<Video> {
        val client = OkHttpClient().newBuilder()
            .connectionSpecs(
                listOf(
                    ConnectionSpec.CLEARTEXT,
                    ConnectionSpec
                        .Builder(
                            ConnectionSpec.MODERN_TLS
                        )
                        .allEnabledTlsVersions()
                        .allEnabledCipherSuites()
                        .build()
                )
            )
            .followRedirects(false)
            .build()

        var response = client
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val urlRequest = PatternManager.singleMatch(
            string =  response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            regex = """file:\s*'([^']*)'""",
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        response = client.newCall(
            GET(
                url = urlRequest,
                headers = hashMapOf(
                    "Referer" to url
                )
            )
        ).await()

        if (!response.isRedirect) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        return listOf(
            Video(
                quality = DEFAULT,
                url = response.header("Location") ?: throw InvalidServerException(context.getString(
                    R.string.server_resource_could_not_find_it, name))
            )
        )
    }
}