package com.ead.lib.moongetter.client.request


data class Request(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val body: Any? = null,
    val asFormUrlEncoded: Boolean = false
)