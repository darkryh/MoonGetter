@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.vk.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.vk.VK
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration

/**
 * Factory responsible for creating instances of the [VK] server.
 *
 * This factory detects and handles video URLs from VK (VKontakte), a Russian social network
 * that hosts embedded video players. The factory uses a regex to match typical VK video embed links.
 */
object VKFactory : Server.Factory {

    /**
     * Unique identifier for this server type.
     */
    override val serverName: String = "VK"

    /**
     * Regex pattern to match VK embedded video URLs, such as:
     * - https://vk.com/video_ext.php?oid=...&id=...
     */
    override val pattern: String = """https:\/\/vk\.com\/video_ext\.php\?[^"\s]+"""

    /**
     * Creates a [VK] server instance prepared to extract and process the given VK video URL.
     *
     * @param url The video URL to handle.
     * @param headers A map of headers to be included in requests.
     * @param configData Configuration data needed by the server.
     * @param client The client responsible for HTTP operations.
     *
     * @return A [VK] server instance ready to process the provided URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = VK(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}