@file:Suppress("UNUSED")

package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.ServerFactoryCreation
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.UserAgent

open class Request(
    builder : Builder
) {

    /**
     * The context of the application.
     */
    @get:JvmName("context") val context: Context? = builder.context


    /**
     * The list of servers to connect to when the programmer provide his own servers.
     */
    @get:JvmName("servers") val servers: Array<Server.Factory> = builder.servers


    /**
     * The headers of the server to connect to.
     */
    @get:JvmName("headers") val headers: HashMap<String, String> = builder.headers



    class Builder() {

        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context? = null

        /**
         * Internal variable to store the list of servers to connect to when the programmer provide his own servers.
         */
        internal var servers : Array<Server.Factory> = emptyArray()


        /**
         * Internal variable to store the headers of the server to connect to.
         */
        internal var headers : HashMap<String, String> = hashMapOf(
            "User-Agent" to UserAgent.value
        )


        /**
         * Setter for the programmer to provide his own servers.
         *
         * example:
         *
         *
         * setCustomServers(
         *
         * listOf(
         *
         *      ServerIntegration(CustomServer1::class.java, "regexPattern1"),
         *
         *      ServerIntegration(CustomServer2::class.java, "regexPattern2"),
         *
         *      ServerIntegration(CustomServer3::class.java, "regexPattern3")
         *
         *      )
         *
         * )
         *
         * provide the configuration to provide your own servers.
         */
        fun onEngine(engines : Array<Server.Factory>) = apply {
            this.servers = engines
        }


        /**
         * Setter to provide own headers.
         */
        fun setHeaders(headers : Map<String, String>) = apply {
            this.headers = HashMap(headers)
        }


        /**
         * Returns a unique identifier name for the given URL and server integrations.
         * This identifier can be used to identify the server in logs and other debugging information.
         *
         * Params:
         *
         * url - The URL of the server.
         *
         * serverIntegrations - A list of server integrations.
         *
         * Returns:
         *
         * A unique identifier name for the server.
         */
        fun identifier(url : String) : String? =
            ServerFactoryCreation.identifier(
                url = url,
                serverIntegrations = servers
            )


        /**
         * Returns a unique identifier name list for the given URL and server integrations. This identifier
         * can be used to identify the server in logs and other debugging information.
         *
         * Params:
         *
         * url - The URL of the server.
         *
         * serverIntegrations - A list of server integrations.
         *
         * Returns: A unique identifier name list for the servers.
         */
        fun identifier(urls : List<String>): List<String> =
            ServerFactoryCreation.identifierList(
                urls = urls,
                serverIntegrations = servers
            )


        /**
         * Return a server that is supported and still functional, even the customs servers,
         * in another case, returns null.
         */
        suspend fun get(url : String) : Server? =
            ServerFactoryCreation.create(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided"),
                /**
                 * Validation url in case it is empty.
                 */
                url =  url.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.url_not_provided) ?:
                        "Url hans´t provided"
                    )
                },
                /**
                 * Pass customs servers
                 */
                serverFactory = servers,
                headers = headers
            )


        /**
         * Return a list of servers that are supported and still functional, even the customs servers,
         * in another case, returns an empty list.
         */
        suspend fun get(urls : List<String>) : List<Server> =
            ServerFactoryCreation.creates(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided"),
                /**
                 * Validation urls in case it is empty.
                 */
                urls = urls.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.urls_not_provided) ?:
                        "Urls hans´t provided"
                    ) },
                /**
                 * Pass oneFichierToken
                 */
                /**
                 * Pass customs servers
                 */
                serverIntegrations = servers,
                headers = headers
            )


        /**
         * Return a server that is supported and still functional, even the customs servers,
         * from an url list, when detect the first functional server, stops searching and returns it.
         * if no server is found, returns null.
         */
        suspend fun getUntilFindResource(urls : List<String>) : Server? =
            ServerFactoryCreation.createUntilFindResource(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided"),
                /**
                 * Validation urls in case it is empty.
                 */
                urls = urls,
                /**
                 * Pass customs servers
                 */
                serverIntegrations = servers,
                headers = headers
            )


        /**
         * Internal constructor that behave as a builder pattern.
         */
        internal constructor(request: Request) : this() {
            this.context = request.context
            this.servers = request.servers
            this.headers = request.headers
        }
    }
}