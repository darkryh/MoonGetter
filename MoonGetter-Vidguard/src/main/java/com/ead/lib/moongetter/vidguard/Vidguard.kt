package com.ead.lib.moongetter.vidguard

import android.content.Context
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.robot.ServerJwPlayer
import okhttp3.OkHttpClient

class Vidguard(
    context: Context,
    url: String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : ServerJwPlayer(context,url,client,headers,configurationData) {
    override val endingRegex: Regex = """.*/favicon\.ico$""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}