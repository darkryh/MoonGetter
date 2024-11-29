package com.ead.lib.moongetter.models

import kotlinx.coroutines.CompletableDeferred
import okhttp3.OkHttpClient

interface Robot {

    suspend fun init(domStorageEnabled : Boolean = true,headers : HashMap<String,String>)

    suspend fun evaluateJavascriptCode(script : String) : String

    suspend fun evaluateJavascriptCodeAndDownload(script : String)

    suspend fun loadUrl(url: String) : Unit?

    suspend fun getInterceptionUrl(url: String, verificationRegex: Regex, endingRegex: Regex,jsCode : String? = null,client: OkHttpClient,configData: Configuration.Data) : String?

    fun requestDeferredResource() : CompletableDeferred<Request?>

    fun release()
}