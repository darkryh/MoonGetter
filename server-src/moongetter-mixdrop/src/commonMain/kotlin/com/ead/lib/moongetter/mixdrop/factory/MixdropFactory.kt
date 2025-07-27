@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.mixdrop.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.mixdrop.Mixdrop
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Mixdrop] server.
 *
 * This factory defines a regex pattern to detect Mixdrop URLs and creates
 * server instances capable of processing those URLs.
 */
object MixdropFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for tracking and logging.
     */
    override val serverName: String = "Mixdrop"

    /**
     * Regex pattern matching Mixdrop URLs of the form:
     * - https://mixdrop.xx/e/abcdef1234
     * - https://mxdrop.xx/e/abcdef1234
     */
    override val pattern: String = """https://m(i)?xdrop\.[a-z]{2,}/e/([a-zA-Z0-9]+)"""

    /**
     * Creates a new [Mixdrop] server instance configured for the provided URL.
     *
     * @param url The URL to be processed.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data required by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Mixdrop] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Mixdrop(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}