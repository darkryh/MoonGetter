package com.ead.lib.moongetter.client.request

import kotlinx.serialization.KSerializer

data class Request<T>(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val body: T? = null,
    val serializer: KSerializer<T>? = null,
    val asFormUrlEncoded: Boolean = false,
    val isResponseBodyNeeded : Boolean = true
)