package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration

/**
 * Configuration holder class for network operations such as timeouts, client, and engines.
 */
class Config(
    val client: MoonClient?,
    val configData: Configuration.Data,
    val factory: Factory.Builder
) {

    /**
     * Builder class for constructing [Config] with a fluent builder pattern.
     */
    class Builder {

        var client: MoonClient? = null
        var configData: Configuration.Data = Configuration.Data()
        var factory: Factory.Builder = Factory.Builder()

        /**
         * Sets the timeout configuration.
         *
         * @param timeoutMillis Timeout value in milliseconds.
         * @return Self reference for chaining.
         */
        fun setTimeout(timeoutMillis: Long) = apply {
            this.configData = this.configData.copy(timeout = timeoutMillis)
        }

        /**
         * Injects the engine to handle resource resolution and scraping logic.
         *
         * @param engine The engine to be used.
         * @return A [Factory.Builder] instance pre-configured.
         */
        fun setEngine(engine: Engine): Factory.Builder {
            factory.configData = this.configData
            factory.engine = engine
            factory.client = client
            factory.client?.initConfigurationData(this.configData)
            return factory
        }

        /**
         * Copies a [Config] instance into the builder.
         */
        constructor(config: Config) {
            this.client = config.client
            this.configData = config.configData
            this.factory = config.factory
        }

        /**
         * Default constructor.
         */
        constructor()
    }
}