@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.uqload.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.uqload.Uqload
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Uqload] server.
 *
 * This factory defines a regex pattern that matches Uqload embed URLs,
 * enabling the creation of server instances capable of handling those URLs.
 */
@OptIn(ExperimentalServer::class)
object UqloadFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used in logging and management.
     */
    override val serverName: String = "Uqload"

    /**
     * Regex pattern matching Uqload embed URLs, for example:
     * - https://uqload.com/embed-abc123.html
     * - https://uqload.net/embed-xyz456.html
     */
    override val pattern: String = """https://uqload\.[a-z]+/embed-[\w\d]+\.html\d*"""

    /**
     * Creates a new [Uqload] server instance configured for the given URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data required by the server.
     * @param client HTTP client for making network requests.
     *
     * @return A configured [Uqload] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Uqload(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}