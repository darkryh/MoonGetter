package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.models.server.Request

data class Video(
    val quality : String?,
    val request: Request
) {
    constructor(
        quality: String?,
        url: String,
        method: String = "GET",
        headers : Map<String, String> ?= null
    ) : this(quality, Request(url = url, method = method, headers = headers))
}