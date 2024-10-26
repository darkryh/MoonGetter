package com.ead.lib.moongetter.models.builder

import android.content.Context
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.utils.UserAgent

class Config(
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
    @get:JvmName("configData") val configData: Configuration.Data = builder.configData


    /**
     * The factory of the server to apply the configurations.
     */
    @get:JvmName("factory") val factory: Factory.Builder = builder.factory

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
         * Internal variable to store the timeout of the server to connect to.
         */
        internal var configData : Configuration.Data = Configuration.Data()


        /**
         * Internal variable to store the factory of the server to apply the configurations.
         */
        internal var factory : Factory.Builder = Factory.Builder()


        /**
         * Setter to provide own headers.
         */
        fun setHeaders(headers : Map<String, String>) = apply {
            this.headers = HashMap(headers)
        }


        /**
         * Setter to provide timeout configurations.
         */
        fun setTimeout(timeoutMillis : Long) = apply {
            this.configData = this.configData.copy(timeout = timeoutMillis )
        }


        /**
         * Setter to provide the engines customs or supported server
         */
        fun setEngine(engine : Engine) : Factory.Builder {


            /**
             * Injects the context of the application.
             */
            factory.context = this.context


            /**
             * Injects the headers of the server to connect to.
             */
            factory.headers = this.headers


            /**
             * Injects the config set on the builder
             */
            factory.configData = this.configData


            /**
             * Injects the factory of the server to apply the configurations.
             */
            factory.engine = engine


            /**
             * Returns the factory of the server to apply the configurations.
             */
            return factory
        }


        internal constructor(config : Config) : this() {
            this.context = config.context
            this.configData = config.configData
            this.factory = config.factory
            this.headers = config.headers
        }
    }
}
