package com.ead.lib.moongetter.doodstream.factory

import com.ead.lib.moongetter.doodstream.Doodstream
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory object responsible for creating instances of the [Doodstream] server.
 *
 * This factory defines a pattern used to recognize URLs supported by the Doodstream server,
 * and creates an appropriate server instance when a URL matches.
 */
object DoodstreamFactory : Server.Factory {

    /**
     * A unique name identifying this server factory.
     * Used for logging, identification, and registration purposes.
     */
    override val serverName: String = "Doodstream"

    /**
     * Regular expression pattern used to match URLs handled by the Doodstream server.
     *
     * Matches URLs like:
     * - https://do123abc.xyz/e/abcdef1234
     * - https://do9xyz.io/e/abcd1234
     */
    override val pattern: String = """https://do[a-zA-Z0-9]*stream?[a-zA-Z0-9]*\.[a-z]{2,}/e/([a-zA-Z0-9]+)"""

    /**
     * Creates a new [Doodstream] server instance configured to handle the given URL.
     *
     * @param url The URL to be processed by the server.
     * @param headers HTTP headers to be used during requests.
     * @param configData Configuration data required for server setup.
     * @param client The HTTP client used to perform network operations.
     *
     * @return An instance of [Doodstream] server ready to process the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Doodstream(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}