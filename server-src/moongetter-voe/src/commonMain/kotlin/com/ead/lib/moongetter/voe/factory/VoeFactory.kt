package com.ead.lib.moongetter.voe.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.voe.Voe
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration

/**
 * Factory responsible for creating instances of the [Voe] server.
 *
 * This factory detects and supports video hosting services powered by Voe or its associated mirrors.
 * These domains often serve embedded video content that can be extracted through MoonGetter.
 */
object VoeFactory : Server.Factory {

    /**
     * Unique identifier for this server type.
     */
    override val serverName: String = "Voe"

    /**
     * Regex pattern used to identify URLs belonging to Voe or its alternative domains.
     * Examples of matching URLs:
     * - https://voe.sx/e/abc123
     * - https://markstyleall.com/d/xyz456
     * - https://cindyeyefinal.sx/e/def789
     */
    override val pattern: String =
        "https?://(?:voe|markstyleall|shannonpersonalcost|cindyeyefinal)\\.(?:com|sx)/(?:e|d)/\\w+"

    /**
     * Creates a [Voe] server instance ready to extract video content from the matched URL.
     *
     * @param url The target video URL.
     * @param headers A map of HTTP headers to include during requests.
     * @param configData Optional configuration values used during extraction.
     * @param client The HTTP client implementation.
     * @return An initialized [Voe] server instance for video extraction.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Voe(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}