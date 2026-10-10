package com.ead.lib.moongetter.models.builder

import com.ead.lib.moongetter.models.Server

/** Configures the server factories used to resolve streaming sources. */
class Engine(val servers: Array<Server.Factory>) {
    class Builder {
        internal var servers: Array<Server.Factory> = emptyArray()

        /** Sets the factories used to match and resolve input URLs. */
        fun onCore(engines: Array<Server.Factory>) = apply {
            servers = engines
        }

        fun build(): Engine = Engine(this)

        internal constructor(engine: Engine) : this() {
            servers = engine.servers
        }

        constructor()
    }

    constructor(builder: Builder) : this(servers = builder.servers)
}
