package com.ead.project.moongetter.domain.custom_servers.sendvid_modified.factory

import com.ead.lib.moongetter.models.Server
import com.ead.project.moongetter.domain.custom_servers.sendvid_modified.SenvidModified
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration

/**
 * Factory responsible for handling the creation of the [SenvidModified] server.
 *
 * This custom factory supports modified Sendvid URLs routed through a custom proxy or service domain.
 * It is used to identify and process links with a specific query format used by custom domain redirections.
 */
object SenvidModifiedFactory : Server.Factory {

    /**
     * The unique name used to represent this server in MoonGetter's registry.
     */
    override val serverName: String = "SenvidModified"

    /**
     * Regex pattern used to match custom-formatted Sendvid links routed through a specific domain.
     *
     * This pattern targets URLs in the format:
     * - https://custom.domain.com/aqua/sv?url=https://sendvid.com/abc123
     *
     * The `url` query parameter contains the actual Sendvid link.
     */
    override val pattern: String = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)"""

    /**
     * Instantiates the [SenvidModified] server with the required parameters.
     *
     * @param url The complete URL containing the proxied Sendvid video link.
     * @param headers Optional headers for the HTTP request.
     * @param configData Configuration data that may include additional flags or contextual information.
     * @param client The client used to handle network operations.
     * @return A fully initialized instance of [SenvidModified].
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = SenvidModified(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}