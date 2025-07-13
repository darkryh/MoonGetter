package com.ead.lib.moongetter.streamwish.factory

import com.ead.lib.moongetter.streamwish.Streamwish
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Streamwish] server.
 *
 * This factory defines a regex pattern to identify Streamwish URLs and
 * creates server instances configured to process those URLs.
 */
object StreamwishFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for logging and management.
     */
    override val serverName: String = "Streamwish"

    /**
     * Regex pattern matching Streamwish URLs such as:
     * - https://[subdomain]wish[subdomain].com/e/abc123
     * - https://swhoi.com/e/abc123
     */
    override val pattern: String = "https?://(?:(?:[\\w-]*wish[\\w-]*)|(?:swhoi))\\.(?:com|to)/e/\\w+"

    /**
     * Creates a new [Streamwish] server instance configured for the specified URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Streamwish] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Streamwish(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}