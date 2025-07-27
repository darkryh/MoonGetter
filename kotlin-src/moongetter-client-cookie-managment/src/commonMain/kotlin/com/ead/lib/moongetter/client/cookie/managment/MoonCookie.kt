package com.ead.lib.moongetter.client.cookie.managment

import io.ktor.http.Cookie
import io.ktor.http.Url

interface MoonCookie {
    interface Management {
        suspend fun get(requestUrl: Url) : List<Cookie>
        suspend fun addCookie(requestUrl: Url, cookie: Cookie)
        fun close()

        companion object Companion {
            fun newEmptyFactory() = object : Management {
                override suspend fun get(requestUrl: Url): List<Cookie> = emptyList()
                override suspend fun addCookie(requestUrl: Url, cookie: Cookie) = Unit
                override fun close() = Unit
            }
        }
    }
}