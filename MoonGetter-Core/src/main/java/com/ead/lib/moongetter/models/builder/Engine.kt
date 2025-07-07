package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.Server

/**
 * Represents an engine that handles how the library communicates with streaming sources.
 * This includes the list of server factories and an optional robot interface to simulate browser behavior.
 */
class Engine(
    val servers: Array<Server.Factory>,
    val robot: Robot?
) {

    /**
     * Builder for configuring and constructing [Engine] instances.
     */
    class Builder {

        /**
         * Internal array of server factories provided by the user.
         */
        internal var servers: Array<Server.Factory> = emptyArray()

        /**
         * Optional robot interface for handling headless browser automation.
         */
        internal var robot: Robot? = null

        /**
         * Sets the array of engine server factories that will be used to resolve streaming sources.
         *
         * Example usage:
         * ```kotlin
         * onCore(arrayOf(OkruFactory(), FacebookFactory(), CustomFactory()))
         * ```
         *
         * @param engines An array of server factories.
         * @return Self reference for chaining.
         */
        fun onCore(engines: Array<Server.Factory>) = apply {
            this.servers = engines
        }

        /**
         * Sets the robot interface to control headless browsing or DOM interactions if needed.
         *
         * @param robot The robot implementation or null.
         * @return Self reference for chaining.
         */
        fun onRobot(robot: Robot?) = apply {
            this.robot = robot
        }

        /**
         * Builds and returns an [Engine] with the current configuration.
         */
        fun build(): Engine = Engine(this)

        /**
         * Internal constructor used to copy an existing engine configuration.
         */
        internal constructor(engine: Engine) : this() {
            this.servers = engine.servers
            this.robot = engine.robot
        }

        /** Default constructor. */
        constructor()
    }

    /**
     * Secondary constructor for internal use to accept a builder instance.
     */
    constructor(builder: Builder) : this(
        servers = builder.servers,
        robot = builder.robot
    )
} // End of Engine class
