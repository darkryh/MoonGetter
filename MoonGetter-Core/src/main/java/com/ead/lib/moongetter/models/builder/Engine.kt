package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.Server

class Engine(
    builder : Builder
) {

    /**
     * The list of servers to connect to when the programmer provide his own servers.
     */
    @get:JvmName("servers") val servers: Array<Server.Factory> = builder.servers

    /**
     * The robot to handle browser operations.
     */
    @get:JvmName("robot") val robot: Robot? = builder.robot


    class Builder() {


        /**
         * Internal variable to store the list of servers to connect to when the programmer provide his own servers.
         */
        internal var servers : Array<Server.Factory> = emptyArray()

        /**
         * Internal variable to store the robot to handle browser operations.
         */
        internal var robot : Robot? = null


        /**
         * Setter for the programmer to engine servers.
         *
         * example:
         *
         *
         * onEngine(
         *
         * arrayOf(
         *
         *      OkruFactory : Server.Factory,
         *      FacebookFactory : Server.Factory,
         *      XTwitterFactory : Server.Factory,
         *      CustomServerFactory : Server.Factory
         *
         *      )
         *
         * )
         *
         * provide the configuration to implement your own or supported servers.
         */
        fun onCore(engines : Array<Server.Factory>) = apply {
            this.servers = engines
        }

        /**
         * Setter for the programmer to provide robot interface
         */
        fun onRobot(robot : Robot?) = apply {
            this.robot = robot
        }


        /**
         * Build the engine with the programmer configuration.
         */
        fun build() = Engine(this)


        /**
         * Internal constructor that behave as a builder pattern.
         */
        internal constructor(engine: Engine) : this() {
            this.servers = engine.servers
        }
    }
}