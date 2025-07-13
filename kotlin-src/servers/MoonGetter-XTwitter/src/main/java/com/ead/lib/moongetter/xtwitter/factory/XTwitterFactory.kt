package com.ead.lib.moongetter.xtwitter.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.xtwitter.XTwitter
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration

/**
 * Factory responsible for creating instances of the [XTwitter] server.
 *
 * This factory supports video extraction from Twitter (now known as X) status pages that contain embedded videos.
 * It identifies matching URLs and instantiates the appropriate [XTwitter] handler.
 */
object XTwitterFactory : Server.Factory {

    /**
     * Unique identifier for this server type.
     */
    override val serverName: String = "XTwitter"

    /**
     * Regular expression pattern used to match supported Twitter/X video URLs.
     *
     * Examples of valid formats:
     * - https://twitter.com/username/status/1234567890
     * - https://x.com/username/status/1234567890
     * - https://x.com/username/status/1234567890/video/1
     */
    override val pattern: String =
        """https?:\/\/(?:www\.)?(?:twitter|x)\.com\/[A-Za-z0-9_]+\/status\/[0-9]+(?:\/video\/[0-9]+)?"""

    /**
     * Creates an instance of [XTwitter] that is capable of handling the matched URL.
     *
     * @param url The full video URL.
     * @param headers HTTP headers to apply in the request (e.g., authentication, user-agent).
     * @param configData Optional configuration information needed by the server.
     * @param client The HTTP client implementation for networking.
     * @return A fully initialized [XTwitter] server instance.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = XTwitter(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}