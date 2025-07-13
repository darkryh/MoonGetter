package com.ead.lib.moongetter.onecloudfile.factory

import com.ead.lib.moongetter.onecloudfile.OneCloudFile
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [OneCloudFile] server.
 *
 * This factory provides a regex pattern to detect URLs hosted on OneCloudFile,
 * and creates server instances to handle content from those URLs.
 */
object OneCloudFileFactory : Server.Factory {

    /**
     * Unique identifier for this server factory, used for logging and management.
     */
    override val serverName: String = "OneCloudFile"

    /**
     * Regex pattern matching URLs of the form:
     * - https://1cloudfile.com/abc123
     */
    override val pattern: String = "https?:\\/\\/1cloudfile\\.com\\/\\w+"

    /**
     * Creates a new [OneCloudFile] server instance configured for the given URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [OneCloudFile] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = OneCloudFile(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}