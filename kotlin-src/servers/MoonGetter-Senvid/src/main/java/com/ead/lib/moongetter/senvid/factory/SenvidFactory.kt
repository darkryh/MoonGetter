package com.ead.lib.moongetter.senvid.factory

import com.ead.lib.moongetter.senvid.Senvid
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Senvid] server.
 *
 * This factory defines a regex pattern to identify Senvid URLs and
 * creates server instances configured to process those URLs.
 */
object SenvidFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for logging and management.
     */
    override val serverName: String = "Senvid"

    /**
     * Regex pattern matching Senvid URLs such as:
     * - https://sendvid.com/abc123
     */
    override val pattern: String = "https?:\\/\\/sendvid\\.com\\/\\w+"

    /**
     * Creates a new [Senvid] server instance configured for the specified URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Senvid] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Senvid(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}