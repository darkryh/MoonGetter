package com.ead.lib.moongetter

import com.ead.lib.moongetter.models.builder.Client
import com.ead.lib.moongetter.models.builder.Initializer

/**
 * Main class for MoonGetter.
 */
open class MoonGetter {

    companion object {


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
        fun Builder() : Client.Builder {
            return Initializer.Builder()
                .initialize()
        }

    }
}