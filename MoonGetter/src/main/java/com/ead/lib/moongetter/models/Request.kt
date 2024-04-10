package com.ead.lib.moongetter.models

import android.content.Context

open class Request(
    builder : Builder
) {

    @get:JvmName("context") val context: Context? = builder.context
    @get:JvmName("url") val url: String = builder.url
    @get:JvmName("config") val config: Config.Builder = builder.config

    class Builder constructor() {
        internal var context : Context?= null
        internal var url : String = ""
        internal var config : Config.Builder = Config.Builder()

        fun connect(url : String) : Config.Builder {
            this.url = url

            config.url = this.url
            config.context = this.context

            return config
        }

        internal constructor(request: Request) : this() {
            this.url = request.url
            this.config = request.config
        }
    }
}