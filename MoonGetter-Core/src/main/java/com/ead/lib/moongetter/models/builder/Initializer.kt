package com.ead.lib.moongetter.models.builder

class Initializer(
    builder : Builder
) {

    /**
     * The request configuration of the connection.
     */
    @get:JvmName("config") val config: Config.Builder = builder.config

    class Builder() {


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
        fun initialize() : Config.Builder { return config }

        internal constructor(initializer: Initializer) : this() {
            this.config = initializer.config
        }
    }
}