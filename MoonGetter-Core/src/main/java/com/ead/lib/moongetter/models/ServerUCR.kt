package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.utils.HttpUtil
import okhttp3.OkHttpClient

open class ServerUCR(
    context: Context,
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(context, url, client, headers, configData) {

    /**
     * the tricky one, disable certification connections
     */
    init {
        HttpUtil.disableCertificationConnections()
    }
}