package com.ead.lib.moongetter.models.builder

/**
 * Entry point for initializing the MoonGetter library.
 * Uses a builder pattern to construct a customizable connection setup.
 */
class Initializer(
    val client: Client.Builder
) {

    /**
     * Builder class for constructing [Initializer] instances.
     * Allows client injection and connection setup.
     */
    class Builder {

        /** Internal storage for the request configuration builder instance. */
        var client: Client.Builder

        /**
         * Default constructor. Initializes an empty client builder.
         */
        constructor() {
            client = Client.Builder()
        }

        /**
         * Secondary constructor. Initializes the builder from an existing [Initializer].
         */
        constructor(initializer: Initializer) {
            client = initializer.client
        }

        /**
         * Returns the [Client.Builder] for further configuration.
         */
        fun initialize(): Client.Builder = client
    }
}