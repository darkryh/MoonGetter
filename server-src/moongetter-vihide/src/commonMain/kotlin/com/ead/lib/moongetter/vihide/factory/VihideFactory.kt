@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.vihide.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.vihide.Vihide
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
import com.ead.lib.moongetter.vihide.VihideHosts

/**
 * Factory responsible for creating instances of the [Vihide] server.
 *
 * This factory uses a regex pattern to detect URLs from Vihide and related domains,
 * enabling the creation of server instances configured to handle those URLs.
 */
@OptIn(ExperimentalServer::class)
object VihideFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for logging and management.
     */
    override val serverName: String = "Vihide"

    /**
     * Routing data for Vidhide, FileLions and their mirrors. Matches URLs like:
     * - https://vidhide.com/v/xyz456
     * - https://callistanise.com/v/jehmieb1epa5
     * - https://niikaplayerr.com/embed/abc123
     *
     * Vidhide runs XFileSharing; its self-hosted /vidhide/ asset path is what tells it apart from
     * Streamwish and Lulustream. Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = VihideHosts.hostSet,
        paths = listOf(PathRule("""/[a-zA-Z0-9]+/([a-zA-Z0-9]+)/?(?:[?#]\S*)?""")),
        fingerprint = Fingerprint(
            Signal.pattern("vidhide.asset-path", SignalKind.UNIQUE, SignalScope.BODY, """["']/vidhide/images/"""),
            Signal.pattern("vidhide.ev-player", SignalKind.SUPPORTING, SignalScope.SCRIPT_SRC, """/assets/jquery/ev-player\.js"""),
        ),
        family = ServerFamily.XFileSharing,
        allowHttp = true,
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern
    /**
     * Creates a new [Vihide] server instance configured for the specified URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client for making network calls.
     *
     * @return A configured [Vihide] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Vihide(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}