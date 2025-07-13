package com.ead.lib.moongetter.pixeldrain.factory

import com.ead.lib.moongetter.pixeldrain.Pixeldrain
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Pixeldrain] server.
 *
 * This factory defines a regex pattern to detect Pixeldrain URLs and
 * creates server instances configured to handle those URLs.
 */
object PixeldrainFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management.
     */
    override val serverName: String = "Pixeldrain"

    /**
     * Regex pattern matching Pixeldrain URLs of the form:
     * - https://pixeldrain.com/u/abc123
     * - https://pixeldrain.com/u/abc123?optional=params
     */
    override val pattern: String = "https?:\\/\\/pixeldrain\\.com\\/u\\/\\w+(?:\\?.*)?"

    /**
     * Creates a new [Pixeldrain] server instance configured for the given URL.
     *
     * @param url The URL to be processed.
     * @param headers Optional HTTP headers to include in requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network operations.
     *
     * @return A configured [Pixeldrain] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Pixeldrain(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}