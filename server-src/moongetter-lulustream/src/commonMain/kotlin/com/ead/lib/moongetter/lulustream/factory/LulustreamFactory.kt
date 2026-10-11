@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.lulustream.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.lulustream.Lulustream
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.ServerFamily
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.lulustream.LulustreamHosts

/**
 * Factory responsible for creating instances of the [Lulustream] server.
 *
 * This factory provides a regex pattern matching URLs served by Lulustream,
 * and creates configured server instances to handle such URLs.
 */
object LulustreamFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management.
     */
    override val serverName: String = "Lulustream"

    /**
     * Routing data for Lulustream. Matches URLs like:
     * - https://luluvdo.com/e/75n0j12zgsl3
     *
     * Lulustream runs XFileSharing, so its page shares most markup with Streamwish and Vidhide; the
     * branded title suffix is what tells it apart. Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = LulustreamHosts.hostSet,
        paths = listOf(PathRule("""/e/([A-Za-z0-9_-]+)/?(?:[?#]\S*)?""")),
        fingerprint = Fingerprint(
            Signal.pattern("lulustream.title-suffix", SignalKind.UNIQUE, SignalScope.TITLE, """(?i)\s-\s*LuluStream\s*$"""),
            Signal.pattern("lulustream.cdn", SignalKind.SUPPORTING, SignalScope.BODY, """//img\.lulucdn\.com/"""),
        ),
        family = ServerFamily.XFileSharing,
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

    /**
     * Creates a new [Lulustream] server instance configured for the given URL.
     *
     * @param url The target URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data required for server operation.
     * @param client HTTP client for network requests.
     *
     * @return A [Lulustream] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Lulustream(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}