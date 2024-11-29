package com.ead.lib.moongetter.vidguard

import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.robot.ServerJwPlayer
import okhttp3.OkHttpClient

class Vidguard(
    url: String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData: Configuration.Data
) : ServerJwPlayer(url,client,headers,configData) {
    override val endingRegex: Regex = """.*/favicon\.ico$""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}