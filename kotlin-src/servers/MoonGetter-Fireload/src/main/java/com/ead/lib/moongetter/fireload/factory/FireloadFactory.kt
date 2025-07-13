package com.ead.lib.moongetter.fireload.factory

import com.ead.lib.moongetter.fireload.Fireload
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Fireload] server.
 *
 * This factory provides a regex pattern to identify URLs served by Fireload
 * and creates server instances capable of handling those URLs.
 */
object FireloadFactory : Server.Factory {

    /**
     * Unique identifier for this server factory, used for logging and identification.
     */
    override val serverName: String = "Fireload"

    /**
     * Regex pattern used to match Fireload URLs.
     *
     * Matches URLs like:
     * - https://fireload.com/...
     * - https://www.fireload.com/...
     */
    override val pattern: String = "https?:\\/\\/(www\\.)?(fireload)\\.com\\/.+"

    /**
     * Creates a new [Fireload] server instance configured with the provided parameters.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data required by the server.
     * @param client The HTTP client to perform network operations.
     *
     * @return A configured instance of [Fireload] server ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Fireload(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}