package com.ead.lib.moongetter.filemoon

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.ServerJwPlayer
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient


class Filemoon(
    context: Context,
    url: String,
    headers : HashMap<String,String>
) : ServerJwPlayer(context,url,headers) {

    override val headers: HashMap<String, String> = headers.also { it.remove("User-Agent") }
    override val endingRegex: Regex = """https:\\/\\/mc\\.yandex\\.ru\\/clmap\\/.*""".toRegex()

    override suspend fun onExtract(): List<Video> {
        val response = OkHttpClient()
            .newCall(GET())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,name))

        url = PatternManager.singleMatch(
            string =  response.body?.string() ?: throw InvalidServerException(context.getString(R.string.server_response_went_wrong, name)),
            regex = """<iframe\s+[^>]*src=["'](https?://[^"']+)["'][^>]*>"""
        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,name))

        return onDefaultJwPlayer()
    }
}