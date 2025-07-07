package com.ead.lib.moongetter.client.cookie.managment

import io.ktor.http.Cookie
import io.ktor.http.Url

interface MoonGetterClientCookieManagement {
    suspend fun get(requestUrl: Url) : List<Cookie>
    suspend fun addCookie(requestUrl: Url, cookie: Cookie)
    fun close()
}