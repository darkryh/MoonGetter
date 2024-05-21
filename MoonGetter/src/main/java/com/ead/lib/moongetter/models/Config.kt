package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.ServerFactory
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

open class Config(
    builder : Builder
) {

    /**
     * The context of the application.
     */
    @get:JvmName("context") val context: Context? = builder.context


    /**
     * The URL of the server to connect to.
     */
    @get:JvmName("url") val url: String = builder.url


    /**
     * The list of URLs of the servers to connect to.
     */
    @get:JvmName("urls") val urls: List<String> = builder.urls


    /**
     * The OneFichier token to connect to the server when 1fichier chose.
     */
    @get:JvmName("oneFichierToken") val oneFichierToken: String? = builder.oneFichierToken


    /**
     * The list of servers to connect to when the programmer provide his own servers.
     */
    @get:JvmName("servers") val servers: List<ServerIntegration> = builder.servers


    class Builder() {

        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context? = null


        /**
         * Internal variable to store the URL of the server to connect to.
         */
        internal var url : String = ""


        /**
         * Internal variable to store the list of URLs of the servers to connect to.
         */
        internal var urls : List<String> = emptyList()


        /**
         * Internal variable to store the OneFichier token to connect to the server when 1fichier chose.
         */
        internal var oneFichierToken : String? = null


        /**
         * Internal variable to store the list of servers to connect to when the programmer provide his own servers.
         */
        internal var servers : List<ServerIntegration> = emptyList()


        /**
         * Setter the 1fichier token to connect to the server.
         *
         *  example:
         *
         *
         * set1FichierToken(
         *
         *  "your_1fichier_token"
         *
         * )
         */
        fun set1FichierToken(token : String) = apply {
            this.oneFichierToken = token
        }



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
        fun setCustomServers(servers : List<ServerIntegration>) = apply {
            this.servers = servers
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
        fun identifier() : String? =
            ServerFactory.identifier(
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
        fun identifierList(): List<String> =
            ServerFactory.identifierList(
                urls = urls,
                serverIntegrations = servers
            )


        /**
         * Return a server that is supported and still functional, even the customs servers,
         * in another case, returns null.
         */
        suspend fun get() : Server? =
            ServerFactory.create(
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
                 * Pass oneFichierToken
                 */
                oneFichierToken = oneFichierToken,
                /**
                 * Pass customs servers
                 */
                serverIntegrations = servers
            )


        /**
         * Return a list of servers that are supported and still functional, even the customs servers,
         * in another case, returns an empty list.
         */
        suspend fun getList() : List<Server> =
            ServerFactory.creates(
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
                oneFichierToken = oneFichierToken,
                /**
                 * Pass customs servers
                 */
                serverIntegrations = servers
            )


        /**
         * Return a server that is supported and still functional, even the customs servers,
         * from an url list, when detect the first functional server, stops searching and returns it.
         * if no server is found, returns null.
         */
        suspend fun getUntilFindResource() : Server? =
            ServerFactory.createUntilFindResource(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided"),
                /**
                 * Validation urls in case it is empty.
                 */
                urls = urls,
                /**
                 * Pass oneFichierToken
                 */
                oneFichierToken = oneFichierToken,
                /**
                 * Pass customs servers
                 */
                serverIntegrations = servers
            )


        /**
         * Internal constructor that behave as a builder pattern.
         */
        internal constructor(config: Config) : this() {
            this.context = config.context
            this.url = config.url
            this.urls = config.urls
            this.oneFichierToken = config.oneFichierToken
            this.servers = config.servers
        }
    }
}