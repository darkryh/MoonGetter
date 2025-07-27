package com.ead.lib.moongetter.facebook.factory

import com.ead.lib.moongetter.facebook.Facebook
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory object responsible for creating instances of the [Facebook] server.
 *
 * This factory identifies URLs matching Facebook video, reel, or watch formats,
 * and provides an instance of the server configured to handle these URLs.
 */
object FacebookFactory : Server.Factory {

    /**
     * A unique name for this factory used for logging and identification.
     */
    override val serverName: String = "Facebook"

    /**
     * Regex pattern to detect Facebook video-related URLs.
     *
     * Matches URLs such as:
     * - https://www.facebook.com/user/videos/1234567890/
     * - https://facebook.com/user/reel/1234567890
     * - https://facebook.com/user/watch/1234567890/
     */
    override val pattern: String = """https?://(www\.)?facebook\.com(?:/[^/]+)?/(videos|reel|watch)(?:/[^/?#]*)?/(\d+)/?"""

    /**
     * Creates a new instance of the [Facebook] server with the provided parameters.
     *
     * @param url The target Facebook URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data necessary for server behavior.
     * @param client HTTP client used to perform network requests.
     *
     * @return An instance of [Facebook] server ready to extract or process content.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Facebook(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}