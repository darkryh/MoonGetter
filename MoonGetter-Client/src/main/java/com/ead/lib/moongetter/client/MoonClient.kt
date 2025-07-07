package com.ead.lib.moongetter.client

import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response

interface MoonClient {
    fun initConfigurationData(configData: Configuration.Data)
    suspend fun <T>request(request: Request<T>): Response
}