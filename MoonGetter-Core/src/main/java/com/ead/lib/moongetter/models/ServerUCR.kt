package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.utils.HttpUtil
import io.ktor.client.HttpClient

open class ServerUCR(
    url : String,
    client: HttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {

    /**
     * the tricky one, disable certification connections
     */
    init {
        HttpUtil.disableCertificationConnections()
    }
}