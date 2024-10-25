package com.ead.lib.moongetter.core

import android.content.Context
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.utils.PatternManager

internal fun Array<Server.Factory>.onFactory(
    context: Context,
    url: String,
    headers: HashMap<String, String>
): Server? {
    return singleOrNull { expectedServerFactory ->
        PatternManager
            .match(
                regex =  expectedServerFactory.pattern,
                string =  url
            )
    }?.let { serverFactory ->
        serverFactory.belongedClass.let { `class` ->
            val constructor = `class`.getDeclaredConstructor(
                Context::class.java,
                String::class.java,
                HashMap::class.java
            )
            constructor.newInstance(context, url, headers)
        }
    }
}