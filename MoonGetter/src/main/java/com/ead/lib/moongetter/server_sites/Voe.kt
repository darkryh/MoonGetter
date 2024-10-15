package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import com.ead.lib.moongetter.utils.decoder
import okhttp3.OkHttpClient
import okhttp3.Request

class Voe(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {
        var response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.VoeIdentifier))

        url = PatternManager.singleMatch(
            string = response.body?.string().toString(),
            regex = """window\.location\.href\s*=\s*'([^']+)"""
        ).toString()

        response = OkHttpClient()
            .newCall(
                Request.Builder()
                    .url(url)
                    .build()
            )
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.VoeIdentifier))

        url = decoder(
            PatternManager.singleMatch(
                string = response.body?.string().toString(),
                regex = """'hls':\s*'([^']+)"""
            ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.VoeIdentifier))
        )

        addDefault()
    }
}