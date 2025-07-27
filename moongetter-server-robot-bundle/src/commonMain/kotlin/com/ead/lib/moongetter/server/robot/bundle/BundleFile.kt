package com.ead.lib.moongetter.server.robot.bundle

import com.ead.lib.moongetter.fireload.factory.FireloadFactory
import com.ead.lib.moongetter.onecloudfile.factory.OneCloudFileFactory

/**
 * bundle of server factories
 */
val serverRobotBundle = arrayOf(
    FireloadFactory,
    OneCloudFileFactory
)