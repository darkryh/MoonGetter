@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.lulustream.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.lulustream.Lulustream
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Lulustream] server.
 *
 * This factory provides a regex pattern matching URLs served by Lulustream,
 * and creates configured server instances to handle such URLs.
 */
object LulustreamFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management.
     */
    override val serverName: String = "Lulustream"

    /**
     * Regex pattern that detects Lulustream URLs in the form:
     * - https://luluXYZ.domain/e/anything
     */
    override val pattern: String = """https://lulu\w*\.\w+/e/[^\s"]+"""

    /**
     * Creates a new [Lulustream] server instance configured for the given URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data required for server operation.
     * @param client HTTP client for network requests.
     *
     * @return A [Lulustream] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Lulustream(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}