package com.ead.lib.moongetter.uqload

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.ServerRobot
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient

@Pending
@Unstable("There is some validations probably in the player side to accept the request")
class Uqload(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : ServerRobot(context,url,headers) {

    override val isDeprecated: Boolean = false

    override suspend fun onExtract() : List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .execute()

        if (!response.isSuccessful) throw throw InvalidServerException(context.getString(R.string.server_domain_is_down, name))

        val body = response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name))


        val headers = mapOf(
            "Referer" to "https://uqload.ws/",
        )

        url = PatternManager.singleMatch(
            string = body,
            regex = "sources:\\s*\\[\"(http[^\\\"]+)"
        )?.takeIf { it.startsWith("http") } ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it, name))

        initializeBrowser(false)

        loadUrlAwait(url)

        evaluateJavascriptCodeAndDownload(
            """
                var xhr_url = $url;
                var xhr = new XMLHttpRequest();
                xhr.open('GET', xhr_url, true);
                xhr.responseType = 'blob';
                xhr.onload = function(){
                    
                    var a = document.createElement('a');
                    var url = window.URL || window.webkitURL;
                    
                    a.href = url.createObjectURL(this.response);
                    a.download =  new Date().getTime()+'.mp4';
                    document.body.appendChild(a);
                    a.click();   
                    a.remove();
                    
                }
                xhr.send();
            """.trimIndent()
        )

        url = requestDeferredResource().await()?.url ?: throw InvalidServerException(context.getString(
            R.string.server_resource_could_not_find_it, name))

        releaseBrowser()

        return listOf(
            Video(
                quality = DEFAULT,
                url = url,
                headers = headers
            )
        )
    }
}