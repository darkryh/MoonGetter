package com.ead.lib.moongetter.mega.factory
import com.ead.lib.moongetter.mega.Mega
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
object MegaFactory:Server.Factory {
    override val serverName="Mega"
    override val pattern="""^https://mega\.nz/(?:file/[A-Za-z0-9_-]{8}#[A-Za-z0-9_-]{43}|folder/[A-Za-z0-9_-]{8}#[A-Za-z0-9_-]{22}(?:/folder/[A-Za-z0-9_-]{8})?(?:/file/[A-Za-z0-9_-]{8})?)$"""
    override fun create(url:String,headers:HashMap<String,String>,configData:Configuration.Data,client:MoonClient):Server=Mega(url,client,headers,configData)
}
