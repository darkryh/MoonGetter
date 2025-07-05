package com.ead.lib.moongetter.cookie

import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI

class JavaNetCookieStorage(
    private val cookieHandler: CookieHandler = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_NONE)
    }
) : CookiesStorage {

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val uri = URI(requestUrl.toString())
        val headers: Map<String, List<String>> = try {
            withContext(Dispatchers.IO) {
                cookieHandler.get(uri, emptyMap())
            }
        } catch (_: IOException) {
            return emptyList()
        }
        val result = mutableListOf<Cookie>()
        for ((key, values) in headers) {
            if (key.equals("Cookie", ignoreCase = true) || key.equals("Cookie2", ignoreCase = true)) {
                for (header in values) {
                    result += decodeHeaderAsJavaNetCookies(requestUrl, header)
                }
            }
        }
        return result
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        val uri = URI(requestUrl.toString())
        val setCookie = cookie.toString()
        val multimap = mapOf("Set-Cookie" to listOf(setCookie))
        try {
            withContext(Dispatchers.IO) {
                cookieHandler.put(uri, multimap)
            }
        } catch (_: IOException) {
            // Log or ignore
        }
    }

    override fun close() = Unit

    private fun decodeHeaderAsJavaNetCookies(requestUrl: Url, header: String): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        var pos = 0
        val limit = header.length
        while (pos < limit) {
            val pairEnd = delimiterOffset(header, ";,", pos, limit)
            val eq = header.indexOf('=', pos).takeIf { it in pos until pairEnd } ?: -1
            val name = header.substring(pos, if (eq != -1) eq else pairEnd).trim()
            var value = if (eq != -1) header.substring(eq + 1, pairEnd).trim() else ""
            if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                value = value.substring(1, value.length - 1)
            }

            cookies += Cookie(
                name = name,
                value = value,
                domain = requestUrl.host,
                path = requestUrl.encodedPath
            )

            pos = pairEnd + 1
        }
        return cookies
    }

    private fun delimiterOffset(input: String, delimiters: String, pos: Int, limit: Int): Int {
        var i = pos
        while (i < limit) {
            if (delimiters.indexOf(input[i]) != -1) return i
            i++
        }
        return limit
    }
}
