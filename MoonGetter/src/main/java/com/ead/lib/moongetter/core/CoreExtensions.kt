package com.ead.lib.moongetter.core

import android.content.Context
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.ServerIntegration
import com.ead.lib.moongetter.utils.PatternManager

fun List<ServerIntegration>.getServerInstance(context: Context, url: String) : Server? {
    return singleOrNull { serverIntegration ->
        PatternManager.match(serverIntegration.pattern, url)
    }?.let { serverIntegration ->
        serverIntegration.serverClass.let { serverClass ->
            val constructor = serverClass.getDeclaredConstructor(Context::class.java,String::class.java)
            constructor.newInstance(context,url)
        }
    }
}