package com.ead.lib.moongetter.client

import com.ead.lib.moongetter.cookie.JavaNetCookieStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.serialization.kotlinx.json.json

class MoonClient {
    val httpClient by lazy {
        HttpClient(CIO) {
            install(HttpCookies) {
                storage = JavaNetCookieStorage()
            }
            install(ContentNegotiation) {
                json()
            }
            followRedirects = true
        }
    }
}