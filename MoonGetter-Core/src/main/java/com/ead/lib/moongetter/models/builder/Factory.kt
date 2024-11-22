@file:Suppress("UNUSED")

package com.ead.lib.moongetter.models.builder

import android.content.Context
import com.ead.lib.moongetter.R
import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.core.MoonFactory
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.Error
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.UserAgent

class Factory(
    builder : Builder
) {

    /**
     * The context of the application.
     */
    @get:JvmName("context") val context: Context? = builder.context


    /**
     * The headers of the server to connect to.
     */
    @get:JvmName("headers") val headers: HashMap<String, String> = builder.headers


    /**
     * The config set on the builder
     */
    @get:JvmName("configData") val timeout: Configuration.Data = builder.configData


    /**
     * The engine of the library
     */
    @get:JvmName("engine") val engine: Engine = builder.engine


    class Builder() {


        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context? = null


        /**
         * Internal variable to store the headers of the server to connect to.
         */
        internal var headers : HashMap<String, String> = hashMapOf(
            "User-Agent" to UserAgent.value
        )


        /**
         * Internal variable to store the timeout of the connection.
         */
        internal var configData : Configuration.Data = Configuration.Data()


        /**
         * Internal variable to store the engines of the library
         */
        internal var engine : Engine = Engine.Builder().build()


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
         * Returns:
         *
         * A unique identifier name for the server.
         */
        fun identifier(url : String) : String? =
            MoonFactory.identifier(
                url =  url.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.url_not_provided) ?:
                        "Url hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    )
                },
                /**
                 * Pass engine servers
                 */
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.engines_not_provided) ?:
                        "Engines hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    )
                }
            )


        /**
         * Returns a unique identifier name list for the given URL and server integrations. This identifier
         * can be used to identify the server in logs and other debugging information.
         *
         * Params:
         *
         * urls - The URLs of the server.
         *
         * Returns: A unique identifier name list for the servers.
         */
        fun identifier(urls : List<String>): List<String> =
            MoonFactory.identifierList(
                context = context ?: throw InvalidServerException("Context hans´t provided",Error.INVALID_BUILDER_PARAMETERS),
                urls = urls.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.urls_not_provided) ?:
                        "Urls hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    ) },
                /**
                 * Pass engine servers
                 */
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.engines_not_provided) ?:
                        "Engines hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    )
                }
            )


        /**
         * Return a server that is supported and still functional, even the customs servers,
         * in another case, returns null.
         */
        suspend fun get(url : String) : Server? =
            MoonFactory.create(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided",Error.INVALID_BUILDER_PARAMETERS),
                /**
                 * Validation url in case it is empty.
                 */
                url =  url.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.url_not_provided) ?:
                        "Url hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    )
                },
                /**
                 * Pass engine servers
                 */
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.engines_not_provided) ?:
                        "Engines hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    )
                },
                /**
                 * Pass headers
                 */
                headers = headers,
                /**
                 * Pass config data
                 */
                configData = configData
            )


        /**
         * Return a list of servers that are supported and still functional, even the customs servers,
         * in another case, returns an empty list.
         */
        @ExperimentalFeature
        suspend fun get(urls : List<String>) : List<Server> =
            MoonFactory.creates(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided",Error.INVALID_BUILDER_PARAMETERS),
                /**
                 * Validation urls in case it is empty.
                 */
                urls = urls.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.urls_not_provided) ?:
                        "Urls hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    ) },
                /**
                 * Pass engine servers
                 */
                serversFactory = engine.servers.ifEmpty {
                    throw InvalidServerException(
                        context?.getString(R.string.engines_not_provided) ?:
                        "Engines hans´t provided",
                        Error.NO_PARAMETERS_TO_WORK
                    )
                },
                /**
                 * Pass headers
                 */
                headers = headers,
                /**
                 * Pass config data
                 */
                configData = configData
            )


        /**
         * Return a server that is supported and still functional, even the customs servers,
         * from an url list, when detect the first functional server, stops searching and returns it.
         * if no server is found, returns null.
         */
        suspend fun getUntilFindResource(urls : List<String>) : Server? =
            MoonFactory.createUntilFindResource(
                /**
                 * The context provided for the ServerFactory object.
                 */
                context = context ?: throw InvalidServerException("Context hans´t provided", Error.INVALID_BUILDER_PARAMETERS),
                /**
                 * Validation urls in case it is empty.
                 */
                urls = urls,
                /**
                 * Pass engine servers
                 */
                serversFactory = engine.servers,
                /**
                 * Pass headers
                 */
                headers = headers,
                /**
                 * Pass config data
                 */
                configData = configData
            )


        internal constructor(factory : Factory) : this() {
            this.context = factory.context
            this.headers = factory.headers
            this.configData = factory.timeout
            this.engine = factory.engine
        }
    }
}
