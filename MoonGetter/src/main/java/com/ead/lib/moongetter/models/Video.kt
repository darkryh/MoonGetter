package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.models.download.Request

data class Video(
    val quality : String?,
    val request: Request
) {
    constructor(
        quality: String?,
        url: String,
        method: String = "GET",
        cookies : Map<String, String> ?= null,
        headers : Map<String, String> ?= null
    ) : this(quality, Request(url = url, method = method, cookies = cookies, headers = headers))
}