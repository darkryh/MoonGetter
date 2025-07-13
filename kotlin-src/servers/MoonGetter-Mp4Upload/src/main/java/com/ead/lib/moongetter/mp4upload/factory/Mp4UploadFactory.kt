@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.mp4upload.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.mp4upload.Mp4Upload
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [Mp4Upload] server.
 *
 * This factory defines a regex pattern to detect Mp4Upload embed URLs,
 * and creates server instances configured to handle those URLs.
 */
object Mp4UploadFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management purposes.
     */
    override val serverName: String = "Mp4Upload"

    /**
     * Regex pattern matching Mp4Upload embed URLs such as:
     * - https://www.mp4upload.xx/embed-xxxxxx.html
     */
    override val pattern: String = """https://www\.mp4upload\.[a-z]+/embed-[\w\d]+\.html"""

    /**
     * Creates a new [Mp4Upload] server instance configured for the specified URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers to be used in requests.
     * @param configData Configuration data required by the server.
     * @param client HTTP client instance to perform network operations.
     *
     * @return A configured [Mp4Upload] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Mp4Upload(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}