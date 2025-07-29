package com.ead.lib.moongetter.client.ktor

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.ktor.util.toHashMap
import com.ead.lib.moongetter.client.ktor.util.toParameters
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.http.HttpMethod as KtorHttpMethod

class KtorMoonClient<out T : HttpClientEngineConfig>(
    engineFactory: HttpClientEngineFactory<T>,
    cookieManagement: MoonCookie.Management,
    private val trustManager: MoonTrust.Manager,
    private val engineConfigBuilder: (T.() -> Unit)? = null
) : MoonClient {

    private val serializer = Json { encodeDefaults = true }

    private val httpClient by lazy {
        HttpClient(engineFactory) {
            engineConfigBuilder?.let { builder ->
                engine(builder)
            }

            install(HttpCookies) {
                storage = object : CookiesStorage {
                    override suspend fun get(requestUrl: io.ktor.http.Url): List<Cookie> {
                        return cookieManagement.get(requestUrl)
                    }

                    override suspend fun addCookie(
                        requestUrl: io.ktor.http.Url,
                        cookie: Cookie
                    ) { cookieManagement.addCookie(requestUrl, cookie) }

                    override fun close() = cookieManagement.close()
                }
            }
            install(ContentNegotiation) {
                json()
            }
            followRedirects = true
        }
    }

    override fun initConfigurationData(configData: Configuration.Data) {
        httpClient.config {
            install(HttpTimeout) {
                requestTimeoutMillis = configData.timeout
                connectTimeoutMillis = configData.timeout
                socketTimeoutMillis = configData.timeout
            }
        }

        if (configData.disableSSLValidation) {
            trustManager.disableCertificationConnections()
        }
    }

    private val forbiddenHeaders = setOf(
        HttpHeaders.TransferEncoding,
        HttpHeaders.ContentLength,
        HttpHeaders.Host,
        HttpHeaders.Connection
    )


    override suspend fun <T> request(request: Request<T>): Response {
        val response = httpClient.request {
            headers {
                request.headers
                    .filterKeys { it !in forbiddenHeaders }
                    .forEach { (key, value) -> set(key, value) }
            }

            method = when (request.method) {
                HttpMethod.GET -> if (request.isResponseBodyNeeded) KtorHttpMethod.Get else KtorHttpMethod.Head
                HttpMethod.POST -> KtorHttpMethod.Post
            }

            url {
                takeFrom(request.url)
                request.queryParams.forEach { (key, value) -> parameters[key] = value }
            }

            if (method == KtorHttpMethod.Post && request.body != null) {
                if (request.asFormUrlEncoded) {
                    contentType(ContentType.Application.FormUrlEncoded)
                    val map = serializer.decodeFromString<Map<String, String>>(
                        serializer.encodeToString(request.serializer ?: error("Missing serializer"), request.body ?: error("Missing body"))
                    )
                    setBody(FormDataContent(toParameters(map, json = serializer)))
                } else {
                    contentType(ContentType.Application.Json)
                    val json = serializer.encodeToString(request.serializer ?: error("Missing serializer"), request.body ?: error("Missing body"))
                    setBody(json)
                }
            }
        }

        val bodyAsString = if (request.isResponseBodyNeeded) response.bodyAsText() else byteArrayOf().decodeToString()

        return object : Response {
            override val statusCode: Int = response.status.value
            override val headers: Map<String, String> = response.headers.toHashMap()
            override val body: ResponseBody = object : ResponseBody {
                override fun asString(): String = bodyAsString
            }

            override val url: Url = object : Url {
                override val toString: String = response.request.url.toString()
                override val host: String = response.request.url.host
            }
        }
    }
}