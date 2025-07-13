@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.filemoon.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.filemoon.Filemoon
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Filemoon] server.
 *
 * This factory provides a regex pattern that matches URLs pointing to Filemoon-hosted content
 * and produces a configured server instance capable of handling such URLs.
 */
object FilemoonFactory : Server.Factory {

    /**
     * A unique identifier for this server factory, useful for logging and tracking.
     */
    override val serverName: String = "Filemoon"

    /**
     * Regex pattern used to identify Filemoon URLs.
     *
     * Matches URLs like:
     * - https://filemoon.sx/abcd1234
     */
    override val pattern: String = "https?://filemoon\\.sx/[^/]+"

    /**
     * Creates a new [Filemoon] server instance configured with the given parameters.
     *
     * @param url The URL to be processed.
     * @param headers HTTP headers to include in network requests.
     * @param configData Configuration data necessary for server operation.
     * @param client The HTTP client used for making requests.
     *
     * @return A [Filemoon] server instance prepared to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Filemoon(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}