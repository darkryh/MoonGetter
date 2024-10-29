package com.ead.lib.moongetter.yourupload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request

@Unstable("server request take too long to respond")
class YourUpload(
    context: Context,
    url : String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {

    override val isDeprecated: Boolean = true

    override val headers: HashMap<String, String> = headers.also { it.remove("User-Agent") }

    override suspend fun onExtract(): List<Video> {
        var response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val urlRequest = PatternManager.singleMatch(
            string =  response.body?.string().toString(),
            regex = """file:\s*'([^']*)'""",
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

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

        if (!response.isRedirect) throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down, name))

        url = response.header("Location") ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))


        return listOf(
            Video(
                quality = DEFAULT,
                url = url.trim(),
                headers = hashMapOf(
                    "Range" to "bytes=0-",
                    "Referer" to "https://www.yourupload.com/"
                )
            )
        )
    }
}