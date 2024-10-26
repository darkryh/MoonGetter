package com.ead.lib.moongetter.vihide

import android.content.Context
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.ServerJwPlayer

class Vihide(
    context: Context,
    url: String,
    headers : HashMap<String,String>,
    configurationData: Configuration.Data
) : ServerJwPlayer(context,url,headers,configurationData) {
    override val endingRegex: Regex = """https:\\/\\/mc\\.yandex\\.ru\\/clmap\\/.*""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}