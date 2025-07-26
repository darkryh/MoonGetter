package com.ead.lib.moongetter.client.ktor.util

import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

internal inline fun <reified T : Any> toParameters(body: T, json: Json): Parameters {
    val element = json.encodeToJsonElement(body)
    require(element is JsonObject) { "toParameters can only process JSON objects, received: $element" }

    return Parameters.build {
        for ((key, value) in element) {
            when (value) {
                is JsonPrimitive -> {
                    append(key, value.content)
                }
                is JsonArray -> {
                    value.forEach { item ->
                        when (item) {
                            is JsonPrimitive -> append(key, item.content)
                            else -> append(key, item.toString())
                        }
                    }
                }
                else -> {
                    append(key, value.toString())
                }
            }
        }
    }
}