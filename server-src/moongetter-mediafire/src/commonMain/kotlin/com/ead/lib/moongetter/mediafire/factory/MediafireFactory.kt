package com.ead.lib.moongetter.mediafire.factory

import com.ead.lib.moongetter.mediafire.Mediafire
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory object responsible for producing instances of the [Mediafire] server.
 *
 * This factory is part of the server discovery mechanism. It defines the pattern
 * that identifies Mediafire URLs and provides a concrete implementation for
 * instantiating the server when a match is found.
 */
object MediafireFactory : Server.Factory {

    /**
     * A unique name identifying this server factory.
     * Useful for tracking, logging, and server registration.
     */
    override val serverName: String = "Mediafire"

    /**
     * The regular expression pattern used to determine whether a given URL
     * is compatible with the Mediafire server implementation.
     *
     * This pattern matches URLs of the form:
     * - https://www.mediafire.com/file/...
     * - https://mediafire.com/?...
     */
    override val pattern: String = """https?:\/\/(?:www\.)?mediafire\.com\/(?:file\/|\?.+)"""

    /**
     * Instantiates a new [Mediafire] server using the provided configuration.
     *
     * This method is invoked when the server factory has identified that a
     * specific URL should be handled by this server.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers to include in requests.
     * @param configData Optional configuration data associated with the context.
     * @param client The HTTP client interface used by the server.
     * @return A fully constructed [Server] capable of handling the provided URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Mediafire(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}