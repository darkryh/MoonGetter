package com.ead.lib.moongetter.models

import android.content.Context
import com.ead.lib.moongetter.core.ServerFactory
import com.ead.lib.moongetter.models.exceptions.InvalidServerException

open class Config(
    builder : Builder
) {

    @get:JvmName("context") val context: Context? = builder.context
    @get:JvmName("url") val url: String = builder.url
    @get:JvmName("oneFichierToken") val oneFichierToken: String? = builder.oneFichierToken

    class Builder constructor() {
        internal var context : Context? = null
        internal var url : String = ""
        internal var oneFichierToken : String? = null

        fun set1FichierToken(token : String) = apply {
            this.oneFichierToken = token
        }

        suspend fun get() : Server =
            ServerFactory.create(
                context = context ?: throw InvalidServerException("Context hans´t provided"),
                url =  url,
                oneFichierToken = oneFichierToken
            )

        internal constructor(config: Config) : this() {
            this.context = config.context
            this.url = config.url
            this.oneFichierToken = config.oneFichierToken
        }
    }
}