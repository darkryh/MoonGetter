package com.ead.lib.moongetter.client.cookie.managment

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url

/** Session-local cookie storage with Ktor's domain, path, secure and expiry matching. */
class IosCookieManagement : MoonCookie.Management {
    private val storage = AcceptAllCookiesStorage()

    override suspend fun get(requestUrl: Url): List<Cookie> = storage.get(requestUrl)

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = storage.addCookie(requestUrl, cookie)

    override fun close() = storage.close()
}
