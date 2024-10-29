package com.ead.lib.moongetter.lulustream

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

@Pending
@Unstable(reason = "Needs to validate requests headers")
class Lulustream(
    context: Context,
    url : String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {

    override val isDeprecated: Boolean = true

    override val headers: HashMap<String, String> = headers.also {
        it["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36"
        it["Referer"] = "https://luluvdo.com"
        it["Origin"] = "https://luluvdo.com"
    }

    override suspend fun onExtract() : List<Video> {
        var response = OkHttpClient()
            .configBuilder()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(com.ead.lib.moongetter.R.string.server_domain_is_down, name))

        val body = response.body?.string() ?: throw InvalidServerException(context.getString(com.ead.lib.moongetter.R.string.server_response_went_wrong, name))

        return listOf(
            Video(
                quality = DEFAULT,
                request = Request(
                    url = PatternManager.singleMatch(
                        string = body,
                        regex = """sources:\s*\[\s*\{file:\s*"(https?://[^"]+)"""
                    )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(context.getString(
                        R.string.server_resource_could_not_find_it, name))
                    ,
                    method = "GET",
                    headers = headers
                )
            )
        )
    }
}