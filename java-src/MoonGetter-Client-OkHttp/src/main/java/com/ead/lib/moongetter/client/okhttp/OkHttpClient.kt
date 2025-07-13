package com.ead.lib.moongetter.client.okhttp

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.okhttp.extensions.await
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.client.trust.manager.java.net.JavaMoonClientTrustManager
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object OkHttpClient : MoonClient {
    private var _httpClient = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            private val cookieStore = HashMap<String, List<Cookie>>()

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val httpClient: OkHttpClient get() = _httpClient

    /**
     * @return the client of okhttp with configurations from the builder
     */
    override fun initConfigurationData(configData: Configuration.Data) {
        _httpClient = _httpClient.newBuilder()
            .let { builder ->
                builder
                    .connectTimeout(
                        configData.timeout,
                        TimeUnit.MILLISECONDS
                    )

                builder.writeTimeout(
                    configData.timeout,
                    TimeUnit.MILLISECONDS
                )

                builder.readTimeout(
                    configData.timeout,
                    TimeUnit.MILLISECONDS
                )
            }
            .build()

        if (configData.disableSSLValidation) {
            JavaMoonClientTrustManager.disableCertificationConnections(forceDisable = false)
        }
    }

    override suspend fun <T> request(request: Request<T>): Response {
        val requestBody = when {
            request.asFormUrlEncoded -> {
                val formBodyBuilder = FormBody.Builder()
                val serializer = request.serializer ?: error("Missing serializer for request body")
                val json = Json.encodeToString(serializer, request.body ?: error("Missing body for request body"))
                val map = Json.decodeFromString<Map<String, String>>(json)
                for ((key, value) in map) {
                    formBodyBuilder.add(key, value)
                }
                formBodyBuilder.build()
            }
            request.body != null -> {
                val serializer = request.serializer ?: error("Missing serializer for request body")
                val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
                val json = Json.encodeToString(serializer, request.body ?: error("Missing body for request body"))
                json.toRequestBody(mediaTypeJson)
            }
            else -> null
        }

        val okhttpRequestBuilder = okhttp3.Request.Builder().url(request.url)

        request.headers.forEach { (key, value) ->
            okhttpRequestBuilder.addHeader(key, value)
        }

        when (request.method) {
            HttpMethod.GET -> okhttpRequestBuilder.get()
            HttpMethod.POST -> okhttpRequestBuilder.post(requestBody ?: ByteArray(0).toRequestBody())
        }

        val call = httpClient.newCall(okhttpRequestBuilder.build())
        val response = call.await()
        val responseBody = response.body.string()

        return object : Response {
            override val statusCode: Int = response.code
            override val headers: Map<String, String> = response.headers.toMap()
            override val body: ResponseBody = object : ResponseBody {
                override fun asString(): String = responseBody
            }
            override val url: Url = object : Url {
                override val toString: String = response.request.url.toString()
                override val host: String = response.request.url.host
            }
        }
    }
}