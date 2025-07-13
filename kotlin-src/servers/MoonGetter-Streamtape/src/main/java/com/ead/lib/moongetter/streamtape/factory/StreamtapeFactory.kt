package com.ead.lib.moongetter.streamtape.factory

import com.ead.lib.moongetter.streamtape.Streamtape
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Streamtape] server.
 *
 * This factory defines a regex pattern to detect Streamtape URLs and
 * creates server instances configured to handle those URLs.
 */
object StreamtapeFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management.
     */
    override val serverName: String = "Streamtape"

    /**
     * Regex pattern matching Streamtape URLs of the form:
     * - https://streamtape.com/e/abc123
     * - https://gettapeads.com/e/abc123
     */
    override val pattern: String = "https?://(?:streamtape|gettapeads)\\.com/e/\\w+"

    /**
     * Creates a new [Streamtape] server instance configured for the specified URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Streamtape] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Streamtape(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}