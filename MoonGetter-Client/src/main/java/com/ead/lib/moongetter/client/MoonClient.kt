package com.ead.lib.moongetter.client

import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response

interface MoonClient {
    suspend fun request(request: Request): Response
}