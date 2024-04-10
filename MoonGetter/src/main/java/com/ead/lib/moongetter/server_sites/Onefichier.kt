package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class Onefichier(context: Context, url : String,val token : String?) : Server(context,url) {

    override suspend fun onExtract() {

        val request = Request.Builder()
            .url(Properties.api1FichierDownloadRequest)
            .header("Authorization", "Bearer ${token?: throw InvalidServerException("1Fichier token hasn't provided")}")
            .header("Content-Type", "application/json")
            .post(getRequestBody())
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).await()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()?:return

            val source = JSONObject(responseBody)
            val status = source.getString("status")
            if (status != "OK") throw InvalidServerException("1Fichier resource couldn't find it")

            url = source.getString("url")
            addDefault()

        }
        else {
            when(response.code) {
                401 -> throw InvalidServerException("1Fichier token expired or incorrect")
                in 500..503 -> throw InvalidServerException("1Fichier domain is Down")
                else -> throw  InvalidServerException("Unexpected request error")
            }
        }
    }

    private fun getRequestBody() : RequestBody {
        val jsonRequest = "{" +
                "\"url\":\"${url}\"," +
                "\"pretty\":1" +
                "}"
        return jsonRequest.toRequestBody(
            "application/json".toMediaTypeOrNull()
        )
    }
}