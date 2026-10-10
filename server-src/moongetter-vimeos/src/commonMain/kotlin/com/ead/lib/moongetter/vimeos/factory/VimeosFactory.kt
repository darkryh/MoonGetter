package com.ead.lib.moongetter.vimeos.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.vimeos.Vimeos

/** Matches Vimeos embed pages, including optional query parameters and fragments. */
object VimeosFactory : Server.Factory {
    override val serverName = "Vimeos"
    override val pattern = """https?://(?:www\.)?vimeos\.net/embed-[A-Za-z0-9]+\.html(?:[?#][^\s]*)?"""

    override fun create(url: String, headers: HashMap<String, String>,
        configData: Configuration.Data, client: MoonClient): Server =
        Vimeos(url, client, headers, configData)
}
