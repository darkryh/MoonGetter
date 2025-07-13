package com.ead.lib.moongetter.goodstream.factory

import com.ead.lib.moongetter.goodstream.GoodStream
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [GoodStream] server.
 *
 * This factory provides a regex pattern that matches GoodStream URLs
 * and instantiates server instances to handle those URLs.
 */
object GoodStreamFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used in logging and server management.
     */
    override val serverName: String = "GoodStream"

    /**
     * Regex pattern to identify URLs served by GoodStream.
     *
     * Matches URLs like:
     * - https://goodstream.example.com/video/abc123
     */
    override val pattern: String = """(https?://goodstream\.[^/]+/video/[^/]+)"""

    /**
     * Creates a new [GoodStream] server instance configured for the provided URL.
     *
     * @param url The target URL for extraction or processing.
     * @param headers Optional HTTP headers to use.
     * @param configData Configuration data for server behavior.
     * @param client HTTP client instance for network requests.
     *
     * @return An instance of [GoodStream] server ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = GoodStream(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}