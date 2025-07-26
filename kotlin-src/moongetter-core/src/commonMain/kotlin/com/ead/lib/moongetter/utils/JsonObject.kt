@file:Suppress("UNCHECKED_CAST")

package com.ead.lib.moongetter.utils

import kotlinx.serialization.json.*

class JsonObject(private val jsonMap: Map<String, Any?>) {

    companion object {

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun fromJson(jsonString: String): JsonObject {
            val jsonElement = json.parseToJsonElement(jsonString)
            val map = convertJsonElementToMap(jsonElement.jsonObject)
            return JsonObject(map)
        }

        private fun convertJsonElementToMap(jsonObj: kotlinx.serialization.json.JsonObject): Map<String, Any?> {
            return jsonObj.entries.associate { (key, value) ->
                key to when (value) {
                    is JsonPrimitive -> {
                        when {
                            value.isString -> value.content
                            value.booleanOrNull != null -> value.boolean
                            value.intOrNull != null -> value.int
                            value.longOrNull != null -> value.long
                            value.floatOrNull != null -> value.float
                            value.doubleOrNull != null -> value.double
                            else -> value.content
                        }
                    }
                    is kotlinx.serialization.json.JsonObject -> convertJsonElementToMap(value)
                    is JsonArray -> value.mapNotNull { item ->
                        when (item) {
                            is JsonPrimitive -> item.contentOrNull ?: item.toString()
                            is kotlinx.serialization.json.JsonObject -> convertJsonElementToMap(item)
                            else -> null
                        }
                    }
                }
            }
        }
    }

    fun getJSONObject(key: String): JsonObject? {
        val nestedMap = jsonMap[key] as? Map<String, Any?>
        return nestedMap?.let { JsonObject(it) }
    }

    fun getJSONArray(key: String): List<JsonObject>? {
        val jsonArray = jsonMap[key] as? List<*> ?: return null
        return jsonArray.mapNotNull {
            (it as? Map<*, *>)?.let { map ->
                JsonObject(map as Map<String, Any?>)
            }
        }
    }

    fun getString(key: String): String? {
        return jsonMap[key] as? String
    }
}