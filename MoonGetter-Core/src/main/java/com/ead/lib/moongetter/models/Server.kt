@file:Suppress("FunctionName","PropertyName")

package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.toParameters
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

open class Server(
    /**
     * @param @url the url of the server
     */
    open var url : String,
    /**
     * @param @client the client of okhttp
     */
    protected val client : HttpClient,
    /**
     * @param @headers the headers of the server
     */
    open val headers : HashMap<String,String>,
    /**
     * @param @configurationData to apply configurations of the library
     */
    protected open val configData : Configuration.Data
) {

    /**
     * @return the robot
     */
    var _robot : Robot? = null


    /**
     * @return the name of the server
     */
    protected val name : String = this::class.java.simpleName



    /**
     * @return the state of the server
     */
    open val isDeprecated : Boolean = false



    /**
     * @return true if server has videos, so it means the resource was found
     */
    internal val isResourceFounded get() = videos.isNotEmpty()



    /**
     * @return true if the server is pending
     */
    internal val isPending = this::class.annotations.any { it::class == Pending::class }



    /**
     * @return list of videos direct link
     */
    val videos : List<Video> get() = _videos
    private val _videos : MutableList<Video> = mutableListOf()



    /**
     * setter for videos
     */
    internal fun setVideos(videos: List<Video>) = this._videos.addAll(videos)


    init {
        // Apply default timeout settings to the client
        client.config {
            install(HttpTimeout) {
                requestTimeoutMillis = configData.timeout
                connectTimeoutMillis = configData.timeout
                socketTimeoutMillis = configData.timeout
            }
        }
    }


    /**
     * onExtract function when the solving or scraping process is getting handle
     *
     * example:
     *
     * fun onExtract() : List<Video> {
     *
     *   do extraction process
     *
     *   ..
     *
     *   ..
     *
     *   when finally resource is found
     *
     *   ..
     *
     *   ..
     *   return listOf(
     *      Video()
     *   )
     *
     * }
     */
    @Throws(InvalidServerException::class,IOException::class, RuntimeException::class, IllegalArgumentException::class)
    open suspend fun onExtract() : List<Video> { return emptyList() }


    /**
     * Perform a GET request.
     * @param requestUrl optional override path or full URL
     * @param overrideHeaders headers to merge with base headers
     * @param queryParams query parameters to append
     * @return response body deserialized as [HttpResponse]
     */
    protected suspend fun HttpClient.GET(
        requestUrl: String? = null,
        overrideHeaders: Map<String, String>? = null,
        queryParams: Map<String, String>? = null
    ): HttpResponse = withContext(Dispatchers.IO) {
        get {
            headers {
                this@Server.headers
                    .filterKeys { it !in forbiddenHeaders }
                    .forEach { (key, value) -> set(key, value) }

                overrideHeaders?.filterKeys { it !in forbiddenHeaders }?.forEach { (key, value) ->
                    set(key, value)
                }
            }
            url {
                val url = requestUrl ?: this@Server.url

                takeFrom(url)

                queryParams?.forEach { (key, value) -> parameters[key] = value }
            }
        }
    }

    /**
     * Perform a POST form request.
     * @param requestUrl optional override url or full URL
     * @param overrideHeaders headers to merge
     * @return response body deserialized as [HttpResponse]
     */
    suspend inline fun <reified T : Any> HttpClient.POST(
        requestUrl: String? = null,
        overrideHeaders: Map<String, String>? = null,
        body: T,
        asFormUrlEncoded: Boolean = false
    ): HttpResponse = withContext(Dispatchers.IO) {
        post {
            headers {
                this@Server.headers.forEach { (key, value) -> set(key, value) }
                overrideHeaders?.forEach { (key, value) -> set(key, value) }
            }

            if (asFormUrlEncoded) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(toParameters(body)))
            } else {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            url {
                takeFrom(requestUrl ?: this@Server.url)
            }
        }
    }

    companion object {
        /**
         * representation of request results
         */
        const val FORBIDDEN = 403
        const val NOT_FOUND = 404


        /**
         * Default name of video result
         */
        const val DEFAULT = "Default"

        val forbiddenHeaders = setOf(
            HttpHeaders.TransferEncoding,
            HttpHeaders.ContentLength,
            HttpHeaders.Host,
            HttpHeaders.Connection
        )
    }


    /**
     * Factory interface for server
     * as a way of implementation for
     * MoonGetter
     */
    interface Factory {



        /**
         * Belonged class of the server that let known to MoonGetter
         * is a server class
         */
        val belongedClass : Class<out Server>


        /**
         * Pattern of the server to let known to MoonGetter
         * when is called to perform extraction
         */
        val pattern : String
    }
}