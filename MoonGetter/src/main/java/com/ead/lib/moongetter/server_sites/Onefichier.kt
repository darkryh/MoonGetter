package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.core.system.extensions.await
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

@Unstable(reason = "Needs to provide a token to work")
class Onefichier(context: Context, url : String,val token : String?) : Server(context,url) {

    override suspend fun onExtract() {

        val request = Request.Builder()
            .url(Properties.api1FichierDownloadRequest)
            .header("Authorization", "Bearer ${(token?: throw InvalidServerException(context.getString(R.string.server_token_has_not_provided,Properties.OneFichierIdentifier))).ifEmpty { throw InvalidServerException(context.getString(R.string.server_token_could_not_be_empty,Properties.OneFichierIdentifier)) }}")
            .header("Content-Type", "application/json")
            .post(getRequestBody())
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).await()

        if (response.isSuccessful) {
            val responseBody = response.body?.string()?:return

            val source = JSONObject(responseBody)
            val status = source.getString("status")
            if (status != "OK") throw InvalidServerException(context.getString(R.string.server_requested_resource_was_taken_down,Properties.OneFichierIdentifier))

            url = source.getString("url")
            addDefault()

        }
        else {
            when(response.code) {
                unauthorized -> throw InvalidServerException(context.getString(R.string.server_token_expired_or_invalid,Properties.OneFichierIdentifier))
                in clientError -> throw InvalidServerException(context.getString(R.string.server_request_wrong_method,Properties.OneFichierIdentifier))
                in serverError -> throw InvalidServerException(context.getString(R.string.server_domain_is_down,Properties.OneFichierIdentifier))
                else -> throw  InvalidServerException(context.getString(R.string.unknown_error))
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