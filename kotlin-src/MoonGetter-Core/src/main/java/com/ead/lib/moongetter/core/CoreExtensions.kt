package com.ead.lib.moongetter.core

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Robot
import com.ead.lib.moongetter.models.Server

/**
 * Attempts to find a matching [Server.Factory] from this array based on a given URL,
 * and uses it to create a corresponding [Server] instance.
 *
 * This function is responsible for routing the URL to the appropriate factory using
 * pattern matching, instantiating the correct server, and optionally attaching a [Robot]
 * if provided.
 *
 * @receiver An array of [Server.Factory] implementations, each capable of handling specific URL patterns.
 *
 * @param url The input URL to be evaluated against all available server factories.
 * @param headers The HTTP headers to be passed into the server instance.
 * @param configData A configuration object that carries metadata or settings required by the server.
 * @param client The network abstraction used for making HTTP requests.
 * @param robot An optional [Robot] instance that may augment or intercept server behavior.
 *
 * @return A new instance of a [Server] created by the matched factory, or `null` if no factory matched the URL.
 */
internal fun Array<Server.Factory>.onFactory(
    url: String,
    headers: HashMap<String, String>,
    configData: Configuration.Data,
    client: MoonClient,
    robot: Robot?
): Server? {
    return singleOrNull { it.pattern.toRegex().containsMatchIn(url) }
        ?.create(url, headers, configData, client)
        ?.apply { this._robot = robot }
}