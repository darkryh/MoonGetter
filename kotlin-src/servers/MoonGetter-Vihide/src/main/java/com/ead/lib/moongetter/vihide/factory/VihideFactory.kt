@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.vihide.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.vihide.Vihide
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Vihide] server.
 *
 * This factory uses a regex pattern to detect URLs from Vihide and related domains,
 * enabling the creation of server instances configured to handle those URLs.
 */
@OptIn(ExperimentalServer::class)
object VihideFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for logging and management.
     */
    override val serverName: String = "Vihide"

    /**
     * Regex pattern matching URLs from Vihide, Filelions, and Niikaplayerr embed URLs, for example:
     * - https://vidhide.com/abc123/xyz456
     * - https://filelions.net/abc123/xyz456
     * - https://niikaplayerr.com/embed/abc123
     */
    override val pattern: String = """(https?://(?:www\.)?(vidhide|filelions)[a-zA-Z]*\.[a-zA-Z]{2,}(?:\.[a-zA-Z]{2,})?/[a-zA-Z0-9]+/[a-zA-Z0-9]+|https?://(?:www\.)?niikaplayerr\.com/embed/[a-zA-Z0-9]+)"""

    /**
     * Creates a new [Vihide] server instance configured for the specified URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client for making network calls.
     *
     * @return A configured [Vihide] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Vihide(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}