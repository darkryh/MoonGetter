package com.ead.lib.moongetter.lamovie.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.lamovie.LaMovie
import com.ead.lib.moongetter.models.Server

/**
 * Factory responsible for creating instances of the [LaMovie] server.
 *
 * This factory uses a regex pattern to detect URLs hosted on Hexload or Hexupload,
 * and creates server instances to handle content from these URLs.
 */
object LaMovieFactory : Server.Factory {

    /**
     * Unique identifier for this server factory, used for tracking and logging.
     */
    override val serverName: String = "LaMovie"

    /**
     * Regex pattern matching URLs from LaMovie  services, such as:
     * - https://lamovie.link/embed-xxxxxx.html
     */
    override val pattern: String = """https://lamovie\.link/embed-[a-zA-Z0-9]+\.html"""

    /**
     * Creates a new [LaMovie] server instance configured for the given URL.
     *
     * @param url The URL to process.
     * @param headers HTTP headers to include in requests.
     * @param configData Configuration data for the server.
     * @param client The HTTP client used for network calls.
     *
     * @return A configured [LaMovie] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = LaMovie(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}