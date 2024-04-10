package com.ead.lib.moongetter.models

import android.content.Context

open class Initializer(
    builder : Builder
) {

    @get:JvmName("context") val context: Context? = builder.context
    @get:JvmName("request") val request: Request.Builder = builder.request

    class Builder constructor() {
        internal var context : Context? = null
        internal var request : Request.Builder = Request.Builder()

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