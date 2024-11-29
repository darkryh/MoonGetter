package com.ead.lib.moongetter.robot

import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Request
import com.ead.lib.moongetter.models.Server
import kotlinx.coroutines.CompletableDeferred
import okhttp3.OkHttpClient

/**
 * Server robot class for sites that needs simulates browser operations
 */
open class ServerRobot(
    url : String,
    client: OkHttpClient,
    headers : HashMap<String,String>,
    configData : Configuration.Data,
) : Server(url, client, headers, configData) {


    private val robot get() = _robot ?: throw IllegalStateException("Robot not initialized")

    protected suspend fun initializeRobot(domStorageEnabled : Boolean = true,headers : HashMap<String,String>)  {
        robot.init(domStorageEnabled,headers)
    }

    protected suspend fun evaluateJavascriptCode(script : String) : String  {
        return robot.evaluateJavascriptCode(script)
    }

    protected suspend fun evaluateJavascriptCodeAndDownload(script : String) {
        robot.evaluateJavascriptCodeAndDownload(script)
    }

    protected suspend fun loadUrl(url: String) : Unit?  {
        return robot.loadUrl(url)
    }

    protected suspend fun getInterceptionUrl(url: String, verificationRegex: Regex, endingRegex: Regex,jsCode : String? = null) : String?  {
        return robot.getInterceptionUrl(url, verificationRegex, endingRegex,jsCode,client,configData)
    }

    protected fun requestDeferredResource() : CompletableDeferred<Request?> {
        return robot.requestDeferredResource()
    }

    protected open fun releaseRobot() = { robot.release() }

}