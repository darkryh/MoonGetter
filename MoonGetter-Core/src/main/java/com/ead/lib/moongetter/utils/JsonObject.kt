@file:Suppress("UNCHECKED_CAST")

package com.ead.lib.moongetter.utils

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class JsonObject(private val jsonMap: Map<String, Any>) {

    companion object {

        /**
         * Moshi instance for parsing JSON
         */
        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        /**
         * Moshi adapter for parsing JSON
         */
        private val jsonAdapter: JsonAdapter<Map<String, Any>> by lazy {
            moshi
                .adapter(Map::class.java)
                    as JsonAdapter<Map<String, Any>>
        }

        /**
         * Parse a JSON string into a JsonObject
         */
        fun fromJson(json: String): JsonObject {
            return JsonObject(
                jsonAdapter.fromJson(json)
                    ?: throw IllegalArgumentException("Error parsing JSON")
            )
        }
    }

    /**
     * Get the Object value associated with the given key
     */
    fun getJSONObject(key: String): JsonObject? {
        val nestedMap = jsonMap[key] as? Map<String, Any>
        return nestedMap?.let { JsonObject(it) }
    }

    /**
     * Get the array value associated with the given key
     */
    fun getJSONArray(key: String): List<JsonObject>? {
        val jsonArray = jsonMap[key] as? List<*> ?: return null
        return jsonArray.mapNotNull {
            (it as? Map<*, *>)?.let { map ->
                JsonObject(map as Map<String, Any>)
            }
        }
    }

    /**
     * Get the value associated with the given key
     */
    fun getString(key: String): String? {
        return jsonMap[key] as? String
    }
}