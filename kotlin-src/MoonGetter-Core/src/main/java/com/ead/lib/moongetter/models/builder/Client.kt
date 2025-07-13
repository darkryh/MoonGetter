package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.client.MoonClient

/**
 * Encapsulates a configurable client setup that is later passed to [Config].
 * This client wraps around a [MoonClient], which can be custom implemented to support different platforms.
 */
class Client(
    val config: Config.Builder
) {

    /**
     * Builder for constructing [Client] instances, allowing the injection of custom [MoonClient].
     */
    class Builder {

        /** Internal configuration builder. */
        var config: Config.Builder

        /** Default constructor. Initializes the default [Config.Builder]. */
        constructor() {
            config = Config.Builder()
        }

        /**
         * Secondary constructor. Copies an existing [Client] configuration.
         */
        constructor(client: Client) {
            config = client.config
        }

        /**
         * Injects a custom [MoonClient] instance.
         *
         * @param client Your own client that conforms to MoonClient abstraction available Ktor or Okhttp.
         * @return A [Config.Builder] to continue the builder chain.
         */
        fun setClient(client: MoonClient): Config.Builder {
            this.config.client = client
            return config
        }
    }
}
