package com.ead.lib.moongetter.gofile.factory
import com.ead.lib.moongetter.gofile.Gofile
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

object GofileFactory : Server.Factory {
    override val serverName = "Gofile"
    override val pattern = """^https://(?:www\.)?gofile\.io/d/[A-Za-z0-9]+/?(?:[?#][^\s]*)?$"""
    override fun create(url: String, headers: HashMap<String,String>, configData: Configuration.Data, client: MoonClient): Server =
        Gofile(url, client, headers, configData)
}
