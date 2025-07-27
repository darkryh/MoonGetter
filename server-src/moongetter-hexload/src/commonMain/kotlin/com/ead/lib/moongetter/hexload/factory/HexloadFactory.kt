package com.ead.lib.moongetter.hexload.factory

import com.ead.lib.moongetter.hexload.Hexload
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Hexload] server.
 *
 * This factory uses a regex pattern to detect URLs hosted on Hexload or Hexupload,
 * and creates server instances to handle content from these URLs.
 */
object HexloadFactory : Server.Factory {

    /**
     * Unique identifier for this server factory, used for tracking and logging.
     */
    override val serverName: String = "Hexload"

    /**
     * Regex pattern matching URLs from Hexload or Hexupload services, such as:
     * - https://hexload.com/embed-xxxxxx.html
     * - https://hexupload.net/embed-yyyyyy.html
     */
    override val pattern: String = "https?://(?:hexload|hexupload)\\.(?:com|net)/embed-[^/]+\\.html"

    /**
     * Creates a new [Hexload] server instance configured for the given URL.
     *
     * @param url The URL to process.
     * @param headers HTTP headers to include in requests.
     * @param configData Configuration data for the server.
     * @param client The HTTP client used for network calls.
     *
     * @return A configured [Hexload] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Hexload(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}