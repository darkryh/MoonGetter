package com.ead.lib.moongetter.streamwish.factory

import com.ead.lib.moongetter.streamwish.Streamwish
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.ServerFamily
import com.ead.lib.moongetter.streamwish.StreamwishHosts

/**
 * Factory responsible for creating instances of the [Streamwish] server.
 *
 * This factory defines a regex pattern to identify Streamwish URLs and
 * creates server instances configured to process those URLs.
 */
object StreamwishFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for logging and management.
     */
    override val serverName: String = "Streamwish"

    /**
     * Routing data for Streamwish and its mirrors. Matches URLs like:
     * - https://streamwish.to/e/abc123
     * - https://swhoi.com/e/abc123
     *
     * Streamwish pages carry no marker that Vidhide or Lulustream pages lack, so it has no fingerprint of
     * its own: on an unknown host it can only be reached through [ServerFamily.XFileSharing] probing.
     * Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = StreamwishHosts.hostSet,
        paths = listOf(PathRule("""/e/(\w+)/?(?:[?#]\S*)?""")),
        family = ServerFamily.XFileSharing,
        allowHttp = true,
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

    /**
     * Creates a new [Streamwish] server instance configured for the specified URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Streamwish] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Streamwish(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}