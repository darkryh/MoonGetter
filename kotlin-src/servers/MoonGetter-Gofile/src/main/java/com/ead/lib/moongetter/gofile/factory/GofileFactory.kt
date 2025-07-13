package com.ead.lib.moongetter.gofile.factory

import com.ead.lib.moongetter.gofile.Gofile
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Gofile] server.
 *
 * This factory defines a regex pattern matching URLs hosted on Gofile,
 * and creates corresponding server instances to handle those URLs.
 */
object GofileFactory : Server.Factory {

    /**
     * Unique identifier for this server factory.
     * Used for logging, debugging, and server management.
     */
    override val serverName: String = "Gofile"

    /**
     * Regex pattern to detect Gofile URLs of the form:
     * - https://gofile.io/d/xxxxxx
     */
    override val pattern: String = """https://gofile\.io/d/(\w+)"""

    /**
     * Creates a new instance of the [Gofile] server using the provided parameters.
     *
     * @param url The URL to be processed by this server.
     * @param headers Optional HTTP headers to include in requests.
     * @param configData Configuration data for server operation.
     * @param client HTTP client used for network communication.
     *
     * @return A fully configured [Gofile] server instance ready for use.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Gofile(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}