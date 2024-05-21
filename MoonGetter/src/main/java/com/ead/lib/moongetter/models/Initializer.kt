package com.ead.lib.moongetter.models

import android.content.Context

open class Initializer(
    builder : Builder
) {

    /**
     * The context of the application.
     */
    @get:JvmName("context") val context: Context? = builder.context


    /**
     * The request configuration of the connection.
     */
    @get:JvmName("request") val request: Request.Builder = builder.request

    class Builder() {


        /**
         * Internal variable to store the context of the application.
         */
        internal var context : Context? = null


        /**
         * Internal variable to store the request configuration of the connection.
         */
        internal var request : Request.Builder = Request.Builder()



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
            this.context = context

            request.context = this.context

            return request
        }

        internal constructor(initializer: Initializer) : this() {
            this.context = initializer.context
            this.request = initializer.request
        }
    }
}