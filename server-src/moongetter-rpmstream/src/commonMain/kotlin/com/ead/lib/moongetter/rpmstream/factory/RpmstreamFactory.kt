package com.ead.lib.moongetter.rpmstream.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
import com.ead.lib.moongetter.rpmstream.RpmstreamHosts
import com.ead.lib.moongetter.rpmstream.Rpmstream

object RpmstreamFactory : Server.Factory {
    override val serverName = "RPMStream"
    /** The page is a generic app shell: its markers support a match but never decide one. */
    override val descriptor = ServerDescriptor(
        hosts = RpmstreamHosts.hostSet,
        paths = listOf(PathRule("""/?#([A-Za-z0-9_-]{2,64})(?:&\S*)?""", prior = PathRule.DISTINCTIVE_PRIOR)),
        fingerprint = Fingerprint(
            Signal.text("rpmstream.vidstack-download-link", SignalKind.SUPPORTING, SignalScope.BODY, "vidstack-download-page-link"),
            Signal.pattern("rpmstream.download-link-script", SignalKind.SUPPORTING, SignalScope.SCRIPT_SRC, """^/download-page-link\.js$"""),
        ),
    )
    override val pattern: String get() = descriptor.pattern
    override fun create(url: String, headers: HashMap<String, String>, configData: Configuration.Data, client: MoonClient): Server =
        Rpmstream(url, client, headers, configData)
}
