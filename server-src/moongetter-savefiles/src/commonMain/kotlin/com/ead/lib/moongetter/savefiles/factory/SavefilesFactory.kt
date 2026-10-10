package com.ead.lib.moongetter.savefiles.factory

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.savefiles.Savefiles

/** Savefiles placeholder embeds; playback is obtained through the site's embed form. */
object SavefilesFactory : Server.Factory {
    override val serverName = "Savefiles"
    override val pattern = """^https?://(?:www\.)?savefiles\.com/e/[A-Za-z0-9]+/?(?:[?#][^\s]*)?$"""

    override fun create(url: String, headers: HashMap<String, String>,
        configData: Configuration.Data, client: MoonClient): Server =
        Savefiles(url, client, headers, configData)
}
