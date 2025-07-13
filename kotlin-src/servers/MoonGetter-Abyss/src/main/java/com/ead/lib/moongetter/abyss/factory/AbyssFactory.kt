package com.ead.lib.moongetter.abyss.factory

import com.ead.lib.moongetter.abyss.Abyss
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

/**
 * Factory object responsible for creating [Abyss] server instances.
 *
 * This factory defines a pattern that matches Abyss URLs and produces
 * a configured [Server] capable of handling those URLs.
 *
 * This implementation is compatible with Kotlin Multiplatform
 * and avoids the use of Java reflection.
 */
object AbyssFactory : Server.Factory {

    /**
     * A unique identifier for this server implementation.
     * Used for internal tracking and debugging.
     */
    override val serverName: String = "Abyss"

    /**
     * Regex pattern that determines whether a given URL
     * should be handled by the [Abyss] server.
     *
     * Matches URLs like: `https://abysscdn.com/?v=...`
     */
    override val pattern: String = """https:\/\/abysscdn\.com\/\?v=[\w-]+"""

    /**
     * Creates a new instance of [Abyss] with the provided parameters.
     *
     * @param url The URL to process.
     * @param headers Optional headers for the request.
     * @param configData Configuration metadata required for the server.
     * @param client HTTP client used for network operations.
     *
     * @return A fully configured [Abyss] server instance.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Abyss(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}