@file:Suppress("FunctionName","PropertyName")

package com.ead.lib.moongetter.models

import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

open class Server(
    /**
     * @param @url the url of the server
     */
    protected open var url : String,
    /**
     * @param @client the client of okhttp
     */
    protected open val client : OkHttpClient,
    /**
     * @param @headers the headers of the server
     */
    protected open val headers : HashMap<String,String>,
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
    @Throws(InvalidServerException::class,IOException::class, RuntimeException::class,
        IllegalArgumentException::class)
    open suspend fun onExtract() : List<Video> { return emptyList() }


    /**
     * @return the client of okhttp with configurations from the builder
     */
    protected fun OkHttpClient.configBuilder() : OkHttpClient {
        return newBuilder()
            .let { builder ->
                /**
                 * set the connection timeout
                 */
                builder
                    .connectTimeout(
                        configData.timeout,
                        TimeUnit.MILLISECONDS
                    )

                /**
                 * set the read timeout
                 */
                builder.writeTimeout(
                    configData.timeout,
                    TimeUnit.MILLISECONDS)

                /**
                 * set the read timeout
                 */
                builder.readTimeout(
                    configData.timeout,
                    TimeUnit.MILLISECONDS
                )
            }
            .build()
    }

    /**
     * Creates and returns an OkHttp GET request.
     *
     * This function allows combining base server headers with optional custom headers,
     * overriding headers if needed, and dynamically appending query parameters to the URL.
     *
     * @param url Optional. If provided, overrides the server's base URL for this request.
     * @param headers Optional. Additional headers to include alongside the server's base headers.
     * @param overrideHeaders Optional. If provided and not in testing mode, completely replaces all headers.
     * @param queryParameters Optional. Query parameters to append to the URL in a safe and structured way.
     * @param isTesting Optional. If true, overrideHeaders will be ignored even if provided.
     *
     * @return Configured OkHttp [Request] object ready to be executed.
     */
    protected fun GET(
        url: String? = null,
        headers: HashMap<String, String>? = null,
        overrideHeaders: Headers? = null,
        queryParameters: Map<String, String>? = null,
        isTesting: Boolean = false
    ): Request {
        return Request.Builder().let { builder ->

            // Combine server-level headers with optional per-request headers
            val headersCombination = if (headers != null) {
                this@Server.headers + headers
            } else {
                this@Server.headers
            }

            // Build the final URL including query parameters if provided
            val finalUrl = (url ?: this.url).let { baseUrl ->
                if (queryParameters.isNullOrEmpty()) {
                    baseUrl
                } else {
                    val httpUrlBuilder = url?.toHttpUrlOrNull()?.newBuilder()
                    queryParameters.forEach { (key, value) ->
                        httpUrlBuilder?.addQueryParameter(key, value)
                    }
                    httpUrlBuilder?.build().toString()
                }
            }

            // Set the URL into the request builder
            builder.url(finalUrl)

            // If overrideHeaders is provided and not testing, apply them directly
            if (overrideHeaders != null && !isTesting) {
                return@let builder.also { it.headers(overrideHeaders) }
            }

            // Otherwise, map and add all combined headers
            builder.headers(
                Headers.Builder().let { headersBuilder ->
                    headersCombination.forEach { (key, value) ->
                        headersBuilder.add(key, value)
                    }
                    headersBuilder
                }.build()
            )
        }.build()
    }


    /**
     * @return the request of okhttp core dependency
     * with POST method and combine headers from the builder
     */
    protected fun POST(
        url : String? = null,
        headers : HashMap<String,String>? = null,
        formBody: FormBody
    )
    : Request {


        /**
         * request that prefab some basic operations
         * that most of servers do
         */
        return Request
            .Builder()
            .let { builder ->


                /**
                 * combine headers in case the POST operations demands
                 */
                val headersCombination = if (headers != null) {
                    this@Server.headers + headers
                } else {
                    this@Server.headers
                }


                /**
                 * set url in case POST operations demands
                 * in other case use the url from the server
                 */
                builder.url(url ?: this.url)


                /**
                 * Do the combining operation
                 */
                builder.headers(
                    Headers
                        .Builder()
                        .let { headersBuilder ->


                            /**
                             * Mapping the headers hashmap into
                             * okhttp headers
                             */
                            headersCombination.forEach { (key, value) ->
                                headersBuilder.add(key, value)
                            }
                            headersBuilder
                        }
                        .build()
                )


                /**
                 * add the FormBody from the parameter
                 */
                builder.post(formBody)
            }
            .build()
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