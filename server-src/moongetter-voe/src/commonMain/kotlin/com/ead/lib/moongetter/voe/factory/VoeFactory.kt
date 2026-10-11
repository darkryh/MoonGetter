package com.ead.lib.moongetter.voe.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.voe.Voe
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.voe.VoeHosts

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
     * Routing data for Voe and its rotating mirrors. Matches URLs like:
     * - https://voe.sx/e/abc123
     * - https://teresapoliticallearn.com/d/xyz456
     *
     * Hosts come from tools/domain-registry/registry.json. The fingerprint lets the optional fallback
     * recognize a Voe player served from a mirror that is not listed yet.
     */
    override val descriptor: ServerDescriptor = ServerDescriptor(
        hosts = VoeHosts.hostSet,
        paths = listOf(PathRule("""/(?:e|d)/(\w+)/?(?:[?#]\S*)?""")),
        fingerprint = Fingerprint(
            Signal.pattern("voe.title-brand", SignalKind.UNIQUE, SignalScope.TITLE, """\bVOE\s*\|\s*Content Delivery Network"""),
            Signal.pattern("voe.loader-script", SignalKind.SUPPORTING, SignalScope.SCRIPT_SRC, """(?:^|/)js/loader\.[0-9a-f]{6,}\.js(?:[?#]|$)"""),
            Signal.pattern("voe.encoded-config", SignalKind.SUPPORTING, SignalScope.BODY, """<script type=["']application/json["']>\s*\[\s*"[A-Za-z0-9+/=^$@~%?*!#&_-]{40,}"""),
        ),
        allowHttp = true,
    )

    /**
     * Regular expression equivalent of [descriptor], kept for code that still reads [pattern].
     */
    override val pattern: String get() = descriptor.pattern

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