@file:Suppress("FunctionName","PropertyName")

package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

open class Server(
    /**
     * @param @url the url of the server
     */
    open var url : String,
    /**
     * @param @client the client of okhttp
     */
    protected val client : MoonClient,
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
    protected val name : String = this::class.simpleName ?: "Unknown"



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
    internal open val isPending = false



    /**
     * @return list of videos direct link
     */
    val videos : List<Video> get() = _videos
    private val _videos : MutableList<Video> = mutableListOf()



    /**
     * setter for videos
     */
    internal fun setVideos(videos: List<Video>) = this._videos.addAll(videos)


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
    @Throws(InvalidServerException::class, RuntimeException::class, IllegalArgumentException::class)
    open suspend fun onExtract() : List<Video> { return emptyList() }


    /**
     * Perform a GET request.
     * @param requestUrl optional override path or full URL
     * @param overrideHeaders headers to merge with base headers
     * @param queryParams query parameters to append
     * @return response body deserialized as [Response]
     */
    protected suspend fun MoonClient.GET(
        requestUrl: String? = null,
        overrideHeaders: Map<String, String>? = null,
        queryParams: Map<String, String>? = null
    ): Response = withContext(Dispatchers.IO) {
        try {
            this@GET.request<String>(
                Request(
                    method = HttpMethod.GET,
                    url = requestUrl ?: this@Server.url,
                    headers = headers.let { headers ->
                        if (overrideHeaders == null) headers
                        else headers + overrideHeaders
                    },
                    queryParams = queryParams ?: emptyMap(),
                    asFormUrlEncoded = false
                )
            )
        }
        catch (e : HttpRequestTimeoutException) {
            e.printStackTrace()
            throw InvalidServerException("timeout on target", Error.TIMEOUT_ERROR)
        }
    }

    /**
     * Perform a POST form request.
     * @param requestUrl optional override url or full URL
     * @param overrideHeaders headers to merge
     * @return response body deserialized as [Response]
     */
    suspend inline fun <reified T : Any> MoonClient.POST(
        requestUrl: String? = null,
        overrideHeaders: Map<String, String>? = null,
        body: T,
        asFormUrlEncoded: Boolean = false
    ): Response = withContext(Dispatchers.IO) {
        try {
            val serializer: KSerializer<T> = serializer()

            this@POST.request(
                Request(
                    method = HttpMethod.POST,
                    url = requestUrl ?: this@Server.url,
                    headers = headers.let { defaultHeaders ->
                        overrideHeaders?.let { defaultHeaders + it } ?: defaultHeaders
                    },
                    body = body,
                    asFormUrlEncoded = asFormUrlEncoded,
                    serializer = serializer
                )
            )
        }
        catch (e : HttpRequestTimeoutException) {
            e.printStackTrace()
            throw InvalidServerException("error", Error.TIMEOUT_ERROR)
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
    }


    /**
     * Factory interface for server
     * as a way of implementation for
     * MoonGetter
     */
    /**
     * Factory interface responsible for providing server implementations
     * used by the MoonGetter library for handling URL-based content extraction.
     *
     * Each implementation of this interface represents a specific type of server,
     * defined by a unique pattern and capable of creating server instances.
     *
     * This version is fully Kotlin Multiplatform (KMP) compatible, avoiding
     * any reflection or JVM-specific class references.
     */
    interface Factory {

        /**
         * A unique name that identifies the server type.
         *
         * This is used internally for logging, debugging, or identifying
         * which server implementation is being used, without requiring class references.
         */
        val serverName: String

        /**
         * The regular expression pattern that this server can handle.
         *
         * MoonGetter uses this pattern to determine which server should
         * process a given URL during the extraction flow.
         */
        val pattern: String

        /**
         * Creates an instance of the associated [Server] implementation.
         *
         * This method is invoked by MoonGetter when a URL matches the [pattern],
         * allowing dynamic and type-safe instantiation of the correct server handler.
         *
         * @param url The URL to be processed by the server.
         * @param headers Optional HTTP headers associated with the request.
         * @param configData Configuration parameters to guide behavior or extraction rules.
         * @param client The HTTP client abstraction used to make network requests.
         * @return A new instance of the appropriate [Server] ready to handle extraction.
         */
        fun create(
            url: String,
            headers: HashMap<String, String>,
            configData: Configuration.Data,
            client: MoonClient,
        ): Server
    }
}