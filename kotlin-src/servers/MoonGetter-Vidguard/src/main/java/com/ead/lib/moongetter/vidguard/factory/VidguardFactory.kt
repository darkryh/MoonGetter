package com.ead.lib.moongetter.vidguard.factory

import com.ead.lib.moongetter.vidguard.Vidguard
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Vidguard] server.
 *
 * This factory defines a regex pattern to detect Vidguard URLs and
 * creates server instances configured to handle those URLs.
 */
object VidguardFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for logging and management.
     */
    override val serverName: String = "Vidguard"

    /**
     * Regex pattern matching Vidguard URLs such as:
     * - https://listeamed.com/e/xyz123
     * - https://vembed.net/d/abc456
     */
    override val pattern: String = "https?://(?:listeamed|vembed)\\.(?:com|net)/(?:e|d)/\\S+"

    /**
     * Creates a new [Vidguard] server instance configured for the specified URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Vidguard] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Vidguard(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}