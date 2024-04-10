package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

class Senvid(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {

        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        url = PatternManager
            .singleMatch(
                string =  response.body?.string().toString(),
                regex =  "<source src=\"(.*?)\""
            ) ?: throw InvalidServerException("Senvid resource couldn't find it")

        addDefault()
    }
}