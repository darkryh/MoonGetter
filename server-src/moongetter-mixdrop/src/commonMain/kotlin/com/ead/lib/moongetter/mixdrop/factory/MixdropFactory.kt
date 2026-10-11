@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.mixdrop.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.mixdrop.Mixdrop
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.mixdrop.MixdropHosts

/**
 * Factory responsible for creating instances of the [Mixdrop] server.
 *
 * This factory defines a regex pattern to detect Mixdrop URLs and creates
 * server instances capable of processing those URLs.
 */
object MixdropFactory : Server.Factory {

    /**
     * Unique identifier for this server factory used for tracking and logging.
     */
    override val serverName: String = "Mixdrop"

    /**
     * Routing data for Mixdrop and its mirrors. Matches URLs like:
     * - https://mixdrop.ag/e/abcdef1234
     * - https://mxdrop.top/e/abcdef1234
     *
     * Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = MixdropHosts.hostSet,
        paths = listOf(PathRule("""/e/([a-zA-Z0-9]+)/?(?:[?#]\S*)?""")),
        fingerprint = Fingerprint(
            Signal.pattern("mixdrop.mdcore", SignalKind.UNIQUE, SignalScope.BODY, """\bMDCore\.(?:ref|sublangs|wurl|vsrc)\b"""),
            Signal.pattern("mixdrop.videoplayer", SignalKind.SUPPORTING, SignalScope.SCRIPT_SRC, """/player/videoplayer[.0-9]*\.min\.js"""),
        ),
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

    /**
     * Creates a new [Mixdrop] server instance configured for the provided URL.
     *
     * @param url The URL to be processed.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data required by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Mixdrop] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Mixdrop(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}