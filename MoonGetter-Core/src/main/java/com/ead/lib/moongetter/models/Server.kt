@file:Suppress("FunctionName","PropertyName")

package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.io.IOException
import java.lang.RuntimeException
import okhttp3.Request
import java.util.concurrent.TimeUnit

open class Server(
    /**
     * @param @context the context of the application
     */
    protected open val context: Context,
    /**
     * @param @url the url of the server
     */
    protected open var url : String,
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
    @Throws(InvalidServerException::class,IOException::class, RuntimeException::class)
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
     * @return the request of okhttp core dependency
     * with GET method and combine headers from the builder
     */
    protected fun GET(
        url : String? = null,
        headers : HashMap<String,String>? = null,
        useHeaders : Boolean = true
    ) : Request {


        /**
         * request that prefab some basic operations
         * that most of servers do
         */
        return Request
            .Builder()
            .let { builder ->


                /**
                 * combine headers in case the GET operations demands
                 */
                val headers = if (headers != null) {
                    this@Server.headers + headers
                } else {
                    this@Server.headers
                }


                /**
                 * set url in case GET operations demands
                 * in other case use the url from the server
                 */
                builder.url(url ?: this.url)


                /**
                 * Do the combining operation
                 */
                if (!useHeaders) return@let builder
                builder.headers(
                    Headers
                        .Builder()
                        .let { headersBuilder ->


                            /**
                             * Mapping the headers hashmap into
                             * okhttp headers
                             */
                            headers.forEach { (key, value) ->
                                headersBuilder.add(key, value)
                            }
                            headersBuilder
                        }
                        .build()
                )
            }
            .build()
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
                val headers = if (headers != null) {
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
                            headers.forEach { (key, value) ->
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
        internal const val FORBIDDEN = 403
        internal const val NOT_FOUND = 404


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