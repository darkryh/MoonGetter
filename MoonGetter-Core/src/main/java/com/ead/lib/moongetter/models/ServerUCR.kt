package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.utils.HttpUtil

open class ServerUCR(
    context: Context,
    url : String,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, headers, configData) {

    /**
     * the tricky one, disable certification connections
     */
    init {
        HttpUtil.disableCertificationConnections()
    }
}