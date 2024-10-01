package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.PatternManager
import okhttp3.OkHttpClient
import okhttp3.Request

@Pending
@Unstable(reason = "Needs to adapt an api identifying new subdomains")
class StreamWish(context: Context, url : String) : Server(context,url) {

    override suspend fun onExtract() {

        val response = OkHttpClient()
            .newCall(Request.Builder().url(url).build())
            .await()

        if (!response.isSuccessful) throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.StreamWishIdentifier))

        url = PatternManager.singleMatch(
            string =  response.body?.string().toString(),
            regex =  """file:"(https://[^"]+)""""
        ) ?: throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.StreamWishIdentifier))

        addDefault()
    }
}