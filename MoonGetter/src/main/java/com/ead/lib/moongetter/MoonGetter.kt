package com.ead.lib.moongetter

import android.content.Context
import com.ead.lib.moongetter.models.Initializer
import com.ead.lib.moongetter.models.Request

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
        fun initialize(context: Context) : Request.Builder {
            return Initializer.Builder()
                .initialize(context)
        }

    }
}