package com.ead.lib.moongetter.abyss.factory
import com.ead.lib.moongetter.abyss.*
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
object AbyssFactory : Server.Factory {
    override val serverName = "Abyss"
    override val pattern = AbyssPayload.PATTERN
    override fun create(url: String, headers: HashMap<String,String>, configData: Configuration.Data, client: MoonClient): Server = Abyss(url,client,headers,configData)
}
