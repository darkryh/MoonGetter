package com.ead.lib.moongetter.doodstream

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Server

@Pending
@Unstable("TODO")
class Doodstream(
    context: Context,
    url: String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : Server(context,url,headers,configurationData) {
    override val isDeprecated: Boolean = true

    /* private var urlTemp: String? = null
    private var rawToken: String? = null
    private var host : String? = null

    override suspend fun onExtract() : List<Video> {
        try {

            val previousHost = URL(url).host

            var request: Request =  Request.Builder().url(url).build()

            var response = OkHttpClient()
                .newCall(request)
                .execute()

            var responseBody = response.body?.string().toString()


            host = response.request.url.host
            url = url.replace(previousHost,host?:return emptyList())

            val keysCode = PatternManager.singleMatch(
                responseBody,
                "dsplayer\\.hotkeys[^']+'([^']+).+?function"
            ).toString()

            urlTemp = "https://$host$keysCode"

            //val requestUrl = url.substringBefore("/e/").plus("/dood")

            Log.d("test", "urlContainer: $urlTemp")

            rawToken = PatternManager.singleMatch(
                responseBody,
                "makePlay.+?return[^?]+([^\"]+)"
            ).toString()

            val hash = urlTemp?.
            substringAfter("pass_md5/")?.
            substringBefore("/")

            Log.d("test", "hash: $hash")

            val token = rawToken?.
            substringAfter("?token=")?.
            substringBefore("&expiry=")

            Log.d("test", "token: $token")


            request = Request.Builder()
                .url(urlTemp?:return emptyList())
                .header("Referer",url)
                .build()

            response = OkHttpClient()
                .newCall(request)
                .execute()

            responseBody = response.body?.string().toString()

            url = responseBody + getRandomString() + rawToken + System.currentTimeMillis() / 1000L

            Log.d("test", "onExtract: $url")

            request = Request.Builder()
                .url(url)
                .headers(doodHeaders(response.request.headers,host?:return emptyList()))
                .build()



            val client: OkHttpClient.Builder = OkHttpClient.Builder()

            response = client
                .build()
                .newCall(
                    request
                )
                .execute()

            val xUrl = response.request.url.toString()

            response = client
                .build()
                .newCall(
                    request
                        .newBuilder()
                        .url(xUrl)
                        .headers(
                            doodHeaders(
                                response.request.headers,
                                host ?: return emptyList()
                            )
                        )
                        .build()
                )
                .execute()

            val code = response.code

            Log.d("test", "onExtract: $code")

            return listOf(
                Video(
                    quality = DEFAULT,
                    url = url,
                    headers = response.request.headers
                        .toMap()
                        .map { it.key to it.value }
                        .toMap(HashMap())
                )
            )
            //Log.d("test", "onExtract: ${response.code}")

        } catch (e : Exception) {
            Log.d("test", "error: ${e.message}")
            return emptyList()
        }
    }

    private fun getRandomString(length: Int = 10): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun doodHeaders(headers: Headers,host: String) : Headers {

        return headers.newBuilder()
            .add("User-Agent", "Aniyomi")
            .add("Referer", "https://$host/")
            .build()
        *//*.toMap()
        .map { it.key to it.value }
        .toMap(HashMap())*//*
    }*/
}