package com.ead.lib.moongetter.doodstream.factory

import com.ead.lib.moongetter.doodstream.Doodstream
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.doodstream.DoodstreamHosts

/**
 * Factory object responsible for creating instances of the [Doodstream] server.
 *
 * This factory defines a pattern used to recognize URLs supported by the Doodstream server,
 * and creates an appropriate server instance when a URL matches.
 */
object DoodstreamFactory : Server.Factory {

    /**
     * A unique name identifying this server factory.
     * Used for logging, identification, and registration purposes.
     */
    override val serverName: String = "Doodstream"

    /**
     * Routing data for Doodstream and its mirrors. Matches URLs like:
     * - https://doodstream.com/e/abcdef1234
     * - https://dsvplay.com/e/j0jlmfjfalnj
     * - https://dood.wf/e/abcd1234
     *
     * Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = DoodstreamHosts.hostSet,
        paths = listOf(PathRule("""/e/([a-zA-Z0-9]+)/?(?:[?#]\S*)?""")),
        fingerprint = Fingerprint(
            Signal.pattern("doodstream.pass-md5", SignalKind.UNIQUE, SignalScope.BODY, """["']/pass_md5/[^"']+["']"""),
            Signal.pattern("doodstream.dsplayer", SignalKind.UNIQUE, SignalScope.BODY, """\bdsplayer\.(?:hotkeys|addRemoteTextTrack)\b"""),
            Signal.pattern("doodstream.make-play", SignalKind.SUPPORTING, SignalScope.BODY, """\bfunction\s+makePlay\s*\("""),
        ),
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

    /**
     * Creates a new [Doodstream] server instance configured to handle the given URL.
     *
     * @param url The URL to be processed by the server.
     * @param headers HTTP headers to be used during requests.
     * @param configData Configuration data required for server setup.
     * @param client The HTTP client used to perform network operations.
     *
     * @return An instance of [Doodstream] server ready to process the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Doodstream(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}