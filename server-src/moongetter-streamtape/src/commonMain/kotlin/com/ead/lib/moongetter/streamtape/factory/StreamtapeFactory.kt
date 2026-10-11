package com.ead.lib.moongetter.streamtape.factory

import com.ead.lib.moongetter.streamtape.Streamtape
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.streamtape.StreamtapeHosts

/**
 * Factory responsible for creating instances of the [Streamtape] server.
 *
 * This factory defines a regex pattern to detect Streamtape URLs and
 * creates server instances configured to handle those URLs.
 */
object StreamtapeFactory : Server.Factory {

    /**
     * Unique identifier for this server factory for logging and management.
     */
    override val serverName: String = "Streamtape"

    /**
     * Routing data for Streamtape and its mirrors. Matches URLs like:
     * - https://streamtape.com/e/abc123
     * - https://gettapeads.com/e/abc123
     *
     * Hosts come from tools/domain-registry/registry.json.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = StreamtapeHosts.hostSet,
        paths = listOf(PathRule("""/e/(\w+)/?(?:[?#]\S*)?""")),
        fingerprint = Fingerprint(
            Signal.pattern("streamtape.robotlink", SignalKind.UNIQUE, SignalScope.BODY, """getElementById\(\s*['"]robotlink['"]\s*\)|\bid=["']robotlink["']"""),
            Signal.pattern("streamtape.plyr-brand", SignalKind.UNIQUE, SignalScope.BODY, """#plyr-streamtape\b|data-plyr=["']streamtape["']"""),
        ),
        allowHttp = true,
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

    /**
     * Creates a new [Streamtape] server instance configured for the specified URL.
     *
     * @param url The URL to process.
     * @param headers Optional HTTP headers for requests.
     * @param configData Configuration data needed by the server.
     * @param client HTTP client used for network communication.
     *
     * @return A configured [Streamtape] server instance ready to handle the URL.
     */
    override fun create(
        url: String,
        headers: HashMap<String, String>,
        configData: Configuration.Data,
        client: MoonClient
    ): Server = Streamtape(
        url = url,
        client = client,
        headers = headers,
        configData = configData
    )
}