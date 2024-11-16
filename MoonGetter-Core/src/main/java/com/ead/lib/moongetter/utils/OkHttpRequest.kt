package com.ead.lib.moongetter.utils

import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private val DEFAULT_HEADERS = Headers.headersOf("Accept", "*/*")
private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().noCache().build()
private val DEFAULT_BODY = "".toRequestBody()


fun GET(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
): Request {
    val httpUrl = url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $url")

    return Request.Builder()
        .url(httpUrl)
        .headers(headers)
        .cacheControl(cache)
        .get()
        .build()
}

/**
 * Creates a GET request for the specified URL with optional headers and cache control.
 *
 * @param url the target URL for the GET request
 * @param headers optional headers to include in the request
 * @param cache optional cache control to use for the request
 * @return an OkHttp Request object for executing the GET request
 */
fun GET(
    url: HttpUrl,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .cacheControl(cache)
        .get()
        .build()
}

/**
 * Creates a POST request for the specified URL with optional headers, request body, and cache control.
 *
 * @param url the target URL for the POST request
 * @param headers optional headers to include in the request
 * @param body optional request body to include in the POST request
 * @param cache optional cache control to use for the request
 * @return an OkHttp Request object for executing the POST request
 */
fun POST(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
): Request {
    return Request.Builder()
        .url(url)
        .headers(headers)
        .cacheControl(cache)
        .post(body)
        .build()
}