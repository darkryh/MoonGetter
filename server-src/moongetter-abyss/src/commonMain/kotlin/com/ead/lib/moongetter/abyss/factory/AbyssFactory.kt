package com.ead.lib.moongetter.abyss.factory
import com.ead.lib.moongetter.abyss.*
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.Fingerprint
import com.ead.lib.moongetter.resolution.PathRule
import com.ead.lib.moongetter.resolution.ServerDescriptor
import com.ead.lib.moongetter.resolution.Signal
import com.ead.lib.moongetter.resolution.SignalKind
import com.ead.lib.moongetter.resolution.SignalScope
object AbyssFactory : Server.Factory {
    override val serverName = "Abyss"
    /** Abyss supports customer domains, so the fingerprint matters more here than the host list. */
    override val descriptor = ServerDescriptor(
        hosts = AbyssHosts.hostSet,
        paths = listOf(
            PathRule("""/\?(?:[^#\s]*&)?v=([A-Za-z0-9_-]{7,17})(?:&[^#\s]*)?""", prior = 10),
            PathRule("""/([A-Za-z0-9_-]{7,17})/?(?:\?[^#\s]*)?"""),
        ),
        fingerprint = Fingerprint(
            Signal.pattern("abyss.sotrym", SignalKind.UNIQUE, SignalScope.BODY, """\bwindow\.SoTrym\b|\bSoTrym\s*="""),
            Signal.pattern("abyss.core-bundle", SignalKind.UNIQUE, SignalScope.SCRIPT_SRC, """/player-v2/core\.bundle\.js"""),
            Signal.pattern("abyss.datas", SignalKind.SUPPORTING, SignalScope.BODY, """\bconst\s+datas\s*=\s*["'][A-Za-z0-9+/=]{40,}"""),
        ),
    )
    override val pattern: String get() = descriptor.pattern
    override fun create(url: String, headers: HashMap<String,String>, configData: Configuration.Data, client: MoonClient): Server = Abyss(url,client,headers,configData)
}
