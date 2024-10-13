package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request

@Unstable("server request take too long to respond")
class YourUpload(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {
        var response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.YourUploadIdentifier))

        val urlRequest = PatternManager.singleMatch(
            string =  response.body?.string().toString(),
            regex = """file:\s*'([^']*)'""",
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.YourUploadIdentifier))


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


        val request = Request.Builder()
            .addHeader("Referer",url)
            .url(urlRequest)

        response = client.newCall(request.build()).await()

        if (!response.isRedirect) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.YourUploadIdentifier))

        url = response.header("Location") ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,Properties.YourUploadIdentifier))

        addDefault()
    }
}