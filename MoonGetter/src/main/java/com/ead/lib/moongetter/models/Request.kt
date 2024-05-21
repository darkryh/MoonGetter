package com.ead.lib.moongetter.models

import android.content.Context

open class Request(
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
     * The URLs of the servers to connect to.
     */
    @get:JvmName("urls") val urls: List<String> = builder.urls


    /**
     * The configuration of the connection.
     */
    @get:JvmName("config") val config: Config.Builder = builder.config


    class Builder() {

        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context?= null


        /**
         * Internal variable to store the URL of the server to connect to.
         */
        internal var url : String = ""


        /**
         * Internal variable to store the URLs of the servers to connect to.
         */
        internal var urls : List<String> = emptyList()


        /**
         * Internal variable to store the configuration of the connection.
         */
        internal var config : Config.Builder = Config.Builder()

        /**
         * Connects to the server using the provided URL.
         *
         * Params:
         *
         * url - The URL to connect to.
         *
         * Returns:
         *
         * A Config.Builder object that can be used to further configure the connection.
         */
        fun connect(url : String) : Config.Builder {
            this.url = url

            config.url = this.url
            config.context = this.context

            return config
        }


        /**
         * Connects to the servers using the provided URLs.
         *
         * Params:
         *
         * urls - The URLs to connect to.
         *
         * Returns:
         *
         * A Config.Builder object that can be used to further configure the connection.
         */
        fun connect(urls : List<String>) : Config.Builder {
            this.urls = urls

            config.urls = this.urls
            config.context = this.context

            return config
        }


        /**
         * Internal constructor that behave as a builder pattern.
         */
        internal constructor(request: Request) : this() {
            this.url = request.url
            this.urls = request.urls
            this.config = request.config
        }
    }
}