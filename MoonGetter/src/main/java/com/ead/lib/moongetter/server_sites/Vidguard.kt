package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.models.ServerJwPlayer

class Vidguard(context: Context,url: String) : ServerJwPlayer(context,url){
    override val endingRegex: Regex = """.*/favicon\.ico$""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}