package com.ead.project.moongetter.domain.custom_servers.test.las_estrellas.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.project.moongetter.domain.custom_servers.test.las_estrellas.LasEstrellas

/**
 * Factory for creating instances of [LasEstrellas], a custom server
 * designed to extract media content from Las Estrellas TV embed URLs.
 *
 * This factory is used by MoonGetter to recognize and handle URLs
 * that follow the Las Estrellas embedding pattern.
 */
object LasEstrellasFactory : Server.Factory {


    /**
     * The name of the server.
     */
    override val serverName: String = "Las Estrellas"

    /**
     * Regex pattern that matches Las Estrellas embed URLs.
     *
     * Example match:
     * https://www.lasestrellas.tv/embed/show-name/episode-name/extra-info
     */
    override val pattern: String = """https://www\.lasestrellas\.tv/embed/[\w-]+/[\w-]+/[\w-]+(?:-[\w-]+)*"""

    /**
     * Creates a new [LasEstrellas] server instance using the given parameters.
     *
     * @param url The URL to be handled by this server.
     * @param headers HTTP headers to be included in the request.
     * @param configData Configuration data needed during extraction.
     * @param client The HTTP client implementation for network access.
     * @return A new [LasEstrellas] server instance.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = LasEstrellas(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}