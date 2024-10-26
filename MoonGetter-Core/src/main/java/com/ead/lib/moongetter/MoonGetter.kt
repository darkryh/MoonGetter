package com.ead.lib.moongetter

import android.content.Context
import com.ead.lib.moongetter.models.builder.Config
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
        fun initialize(context: Context) : Config.Builder {
            return Initializer.Builder()
                .initialize(context)
        }

    }
}