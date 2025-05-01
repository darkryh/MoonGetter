@file:Suppress("RestrictedApi")

package com.ead.lib.moongetter.filemoon

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.robot.ServerJwPlayer
import okhttp3.OkHttpClient

@ExperimentalServer
class Filemoon(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : ServerJwPlayer(url,client,headers,configData) {
    override val endingRegex: Regex = """.*/favicon\.ico$""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}