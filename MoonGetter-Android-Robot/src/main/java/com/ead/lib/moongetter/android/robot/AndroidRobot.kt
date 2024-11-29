package com.ead.lib.moongetter.android.robot

import com.ead.lib.moongetter.android.robot.models.builder.Config

/**
 * [AndroidRobot] class as robot option for android devices.
 * avoid to use in Java-Kotlin project without android environment.
 */
class AndroidRobot {

    companion object {

        /**
         * Creates a new [Config.Builder] instance.
         */
        fun Builder() : Config.Builder {
            return Config.Builder()
        }
    }
}