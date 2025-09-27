package com.ead.project.moongetter.presentation.util

import com.ead.lib.moongetter.server.bundle.serverBundle
import com.ead.lib.moongetter.server.robot.bundle.serverRobotBundle

object DataUtil {
    val serversNames = serverBundle.map { it.serverName } + serverRobotBundle.map { it.serverName }
}