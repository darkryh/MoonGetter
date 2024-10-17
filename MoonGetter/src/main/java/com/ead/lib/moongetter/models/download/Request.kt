package com.ead.lib.moongetter.models.download

data class Request(
    val url: String,
    val method: String,
    val headers : Map<String, String>?,
)