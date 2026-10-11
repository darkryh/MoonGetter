@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.filemoon.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.filemoon.Filemoon
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.filemoon.FilemoonHosts

/**
 * Factory responsible for creating instances of the [Filemoon] server.
 *
 * This factory provides a regex pattern that matches URLs pointing to Filemoon-hosted content
 * and produces a configured server instance capable of handling such URLs.
 */
object FilemoonFactory : Server.Factory {

    /**
     * A unique identifier for this server factory, useful for logging and tracking.
     */
    override val serverName: String = "Filemoon"

    /**
     * Routing data for Filemoon and its Byse frontends. Matches URLs like:
     * - https://filemoon.sx/abcd1234
     * - https://filemoon.sx/e/abcd1234
     * - https://bysekoze.com/e/y6kmotbkxiha/
     *
     * Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = FilemoonHosts.hostSet,
        paths = listOf(
            PathRule("""/e/(\w+)/?(?:[?#]\S*)?"""),
            PathRule("""/(\w+)/?(?:[?#]\S*)?"""),
        ),
        fingerprint = Fingerprint(
            Signal.pattern("byse.title", SignalKind.UNIQUE, SignalScope.TITLE, """^Byse Frontend$"""),
            Signal.text("byse.embed-mode", SignalKind.SUPPORTING, SignalScope.BODY, "video-embed-mode"),
        ),
        allowHttp = true,
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

    /**
     * Creates a new [Filemoon] server instance configured with the given parameters.
     *
     * @param url The URL to be processed.
     * @param headers HTTP headers to include in network requests.
     * @param configData Configuration data necessary for server operation.
     * @param client The HTTP client used for making requests.
     *
     * @return A [Filemoon] server instance prepared to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Filemoon(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}