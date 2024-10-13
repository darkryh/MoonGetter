package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.ServerJwPlayer
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class Filemoon(context: Context,url: String) : ServerJwPlayer(context,url) {
    override val identifier: String? = Properties.FilemoonIdentifier
    override val endingRegex: Regex = """https:\\/\\/mc\\.yandex\\.ru\\/clmap\\/.*""".toRegex()

    override suspend fun onExtract() {
        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.FilemoonIdentifier))

        url = PatternManager.singleMatch(
            string =  response.body?.string().toString(),
            regex = """<iframe\s+[^>]*src=["'](https?://[^"']+)["'][^>]*>"""
        ) ?: throw InvalidServerException(context.getString(R.string.server_resource_could_not_find_it,Properties.FilemoonIdentifier))

        onDefaultJwPlayer()
    }
}