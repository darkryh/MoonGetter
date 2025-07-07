package com.ead.lib.moongetter.core

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.utils.PatternManager

internal fun Array<Server.Factory>.onFactory(
    url: String,
    headers: HashMap<String, String>,
    configData: Configuration.Data,
    client: MoonClient,
    robot: Robot?
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
                String::class.java,
                MoonClient::class.java,
                HashMap::class.java,
                Configuration.Data::class.java
            )
            constructor.newInstance(url, client, headers, configData).also {
                robot?.let { robot ->
                    it._robot = robot
                }
            }
        }
    }
}