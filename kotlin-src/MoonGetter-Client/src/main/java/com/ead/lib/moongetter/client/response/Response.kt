package com.ead.lib.moongetter.client.response

import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url


interface Response {
    val statusCode: Int
    val headers: Map<String, String>
    val body: ResponseBody
    val url : Url

    val isSuccess: Boolean get() = statusCode in 200..299
}