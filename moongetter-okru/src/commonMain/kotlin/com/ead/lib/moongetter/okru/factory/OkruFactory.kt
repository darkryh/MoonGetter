package com.ead.lib.moongetter.okru.factory

import com.ead.lib.moongetter.okru.Okru
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Okru] server.
 *
 * This factory defines a regex pattern to identify Okru video embed URLs,
 * and creates server instances configured to handle those URLs.
 */
object OkruFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management.
     */
    override val serverName: String = "Okru"

    /**
     * Regex pattern matching Okru video embed URLs such as:
     * - https://ok.ru/videoembed/xxxxxxx
     */
    override val pattern: String = "https?:\\/\\/(www\\.)?ok\\.ru\\/videoembed\\/.+"

    /**
     * Creates a new [Okru] server instance configured for the given URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for network requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for making requests.
     *
     * @return A configured [Okru] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Okru(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}