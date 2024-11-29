package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.models.Configuration

class Config(
    builder : Builder
) {

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
         * Internal variable to store the timeout of the server to connect to.
         */
        internal var configData : Configuration.Data = Configuration.Data()


        /**
         * Internal variable to store the factory of the server to apply the configurations.
         */
        internal var factory : Factory.Builder = Factory.Builder()


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
            this.configData = config.configData
            this.factory = config.factory
        }
    }
}
