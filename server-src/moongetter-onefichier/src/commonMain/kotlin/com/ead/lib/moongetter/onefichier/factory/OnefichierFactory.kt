package com.ead.lib.moongetter.onefichier.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.onefichier.Onefichier
import com.ead.lib.moongetter.onefichier.OnefichierPage

object OnefichierFactory : Server.Factory {
    override val serverName = "1fichier"
    override val pattern = OnefichierPage.PATTERN
    override fun create(url: String, headers: HashMap<String, String>, configData: Configuration.Data, client: MoonClient): Server =
        Onefichier(url, client, headers, configData)
}
