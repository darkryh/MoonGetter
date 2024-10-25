package com.ead.lib.moongetter.streamtape

import android.content.Context
import com.ead.lib.moongetter.models.ServerJwPlayer

class Streamtape(
    context: Context,
    url : String,
    headers : HashMap<String,String>
) : ServerJwPlayer(context,url,headers) {
    override val interceptionRegex = """https://streamtape\.com/get_video\?id=[\w-]+&expires=\d+&ip=[\w-]+&token=[\w-]+&stream=\d+""".toRegex()
    override val endingRegex = """https://streamtape\.com/favicon\.ico""".toRegex()
    override fun scriptLoader(): String? = null

    override suspend fun onExtract() = onDefaultJwPlayer()
}