package com.ead.lib.moongetter.vidguard

import android.content.Context
import com.ead.lib.moongetter.models.ServerJwPlayer

class Vidguard(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : ServerJwPlayer(context,url,headers) {
    override val endingRegex: Regex = """.*/favicon\.ico$""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}