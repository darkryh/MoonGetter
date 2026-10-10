package com.ead.lib.moongetter.rpmstream.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.rpmstream.RpmLink
import com.ead.lib.moongetter.rpmstream.Rpmstream

object RpmstreamFactory : Server.Factory {
    override val serverName = "RPMStream"
    override val pattern = RpmLink.PATTERN
    override fun create(url: String, headers: HashMap<String, String>, configData: Configuration.Data, client: MoonClient): Server =
        Rpmstream(url, client, headers, configData)
}
