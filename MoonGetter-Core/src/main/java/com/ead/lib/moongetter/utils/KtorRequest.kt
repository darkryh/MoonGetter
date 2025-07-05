@file:Suppress("unused")

package com.ead.lib.moongetter.utils

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.reflect.full.memberProperties


/**
 * Default headers equivalent to Accept:
 */
private val DEFAULT_HEADERS: Map<String, String> = mapOf("Accept" to "*/*")

/**
 * Builds a GET request in Ktor using HttpRequestBuilder.
 *
 * @param url the target URL
 * @param headers optional headers to include
 * @param cacheControl optional cache control directive (used in Cache-Control header)
 */
fun GET(
    url: String,
    headers: Map<String, String> = DEFAULT_HEADERS,
    cacheControl: String? = "no-cache"
): HttpRequestBuilder {
    return HttpRequestBuilder().apply {
        method = HttpMethod.Get
        url(url)
        headers {
            headers.forEach { (k, v) -> append(k, v) }
            cacheControl?.let { append(HttpHeaders.CacheControl, it) }
        }
    }
}

/**
 * Builds a POST request in Ktor using HttpRequestBuilder.
 *
 * @param url the target URL
 * @param headers optional headers to include
 * @param body optional body content (usually string or JSON)
 * @param contentType content type for the body (defaults to text/plain)
 * @param cacheControl optional cache control directive (used in Cache-Control header)
 */
fun POST(
    url: String,
    headers: Map<String, String> = DEFAULT_HEADERS,
    body: Any = "",
    contentType: ContentType = ContentType.Text.Plain,
    cacheControl: String? = "no-cache"
): HttpRequestBuilder {
    return HttpRequestBuilder().apply {
        method = HttpMethod.Post
        url(url)
        setBody(body)
        this.contentType(contentType)
        headers {
            headers.forEach { (k, v) -> append(k, v) }
            cacheControl?.let { append(HttpHeaders.CacheControl, it) }
        }
    }
}

val emptyHeaders = Headers.build {}

fun Headers.toHashMap(): HashMap<String, String> {
    return HashMap<String, String>().apply {
        this@toHashMap.entries().forEach { (key, values) ->
            put(key, values.firstOrNull().orEmpty())
        }
    }
}

fun HashMap<String, String>.toHeaders(): Headers {
    return Headers.build {
        this@toHeaders.forEach { (key, value) ->
            append(key, value)
        }
    }
}

fun String.toHttpUrl(): Url = Url(this)

/**
 * Convierte cualquier data class serializable en Parameters para un form-url-encoded.
 */
inline fun <reified T : Any> toParameters(body: T, json: Json = Json { encodeDefaults = true }): Parameters {
    // 1. Serializamos el objeto a JsonElement
    val element = json.encodeToJsonElement(body)
    require(element is JsonObject) { "toParameters sólo puede procesar objetos JSON, recibí: $element" }

    // 2. Iteramos cada par clave->valor
    return Parameters.build {
        for ((key, value) in element) {
            when (value) {
                is JsonPrimitive -> {
                    // JsonPrimitive.content respeta strings, números y booleans
                    append(key, value.content)
                }
                is JsonArray -> {
                    // Si es lista, agregamos una entrada por elemento
                    value.forEach { item ->
                        when (item) {
                            is JsonPrimitive -> append(key, item.content)
                            else               -> append(key, item.toString())
                        }
                    }
                }
                else -> {
                    // Objetos anidados u otros casos, enviamos toString()
                    append(key, value.toString())
                }
            }
        }
    }
}