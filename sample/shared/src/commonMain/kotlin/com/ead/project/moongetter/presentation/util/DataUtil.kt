package com.ead.project.moongetter.presentation.util

import com.ead.lib.moongetter.server.bundle.serverBundle

object DataUtil {
    val serversNames = serverBundle.map { it.serverName }
}
