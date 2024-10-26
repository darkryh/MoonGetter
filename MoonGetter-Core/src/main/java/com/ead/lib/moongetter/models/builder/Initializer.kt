package com.ead.lib.moongetter.models.builder

import android.content.Context

class Initializer(
    builder : Builder
) {

    /**
     * The context of the application.
     */
    @get:JvmName("context") val context: Context? = builder.context


    /**
     * The request configuration of the connection.
     */
    @get:JvmName("config") val config: Config.Builder = builder.config

    class Builder() {


        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context? = null


        /**
         * Internal variable to store the request configuration of the connection.
         */
        internal var config : Config.Builder = Config.Builder()



        /**
         * Initializes a new Request.Builder object with the provided context.
         *
         * Params:
         *
         * context - The context of the application.
         *
         * Returns:
         *
         * The Request.Builder object.
         */
        fun initialize(context: Context) : Config.Builder {
            this.context = context

            config.context = this.context

            return config
        }

        internal constructor(initializer: Initializer) : this() {
            this.context = initializer.context
            this.config = initializer.config
        }
    }
}