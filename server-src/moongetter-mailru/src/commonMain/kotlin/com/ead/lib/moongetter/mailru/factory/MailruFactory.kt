package com.ead.lib.moongetter.mailru.factory
import com.ead.lib.moongetter.mailru.Mailru
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server

object MailruFactory:Server.Factory {
    override val serverName="Mailru"
    override val pattern="""^https://my\.mail\.ru/video/embed/[0-9]+/?(?:[?#][^\s]*)?$"""
    override fun create(url:String,headers:HashMap<String,String>,configData:Configuration.Data,client:MoonClient):Server=Mailru(url,client,headers,configData)
}
