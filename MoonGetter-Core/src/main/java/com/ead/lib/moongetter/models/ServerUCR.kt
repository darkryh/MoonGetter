package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.utils.HttpUtil

open class ServerUCR(
    url : String,
    client: MoonClient,
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