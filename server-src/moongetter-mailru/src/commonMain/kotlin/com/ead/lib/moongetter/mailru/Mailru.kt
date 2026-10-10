@file:Suppress("RestrictedApi", "VisibleForTests")
package com.ead.lib.moongetter.mailru

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.ktor.http.Url
import kotlinx.serialization.json.*

/** Resolves the embed metadata and forwards only the provider's playback cookie. */
class Mailru(url: String, client: MoonClient, headers: HashMap<String,String>, configData: Configuration.Data) :
    Server(url,client,headers,configData) {
    override suspend fun onExtract(): List<Video> {
        val embed=url.substringBefore('#')
        val id=Regex("""^https://my\.mail\.ru/video/embed/([0-9]+)/?(?:\?[^\s]*)?$""").find(embed)?.groupValues?.get(1)
            ?: fail("Invalid Mail.ru embed URL")
        val requestHeaders=headers + mapOf("Referer" to embed,"Origin" to "https://my.mail.ru")
        val page=client.GET(requestUrl=embed,overrideHeaders=requestHeaders)
        if(!page.isSuccess) fail("Mail.ru embed HTTP ${page.statusCode}",page.statusCode)
        val metadata=MailruPayload.metadata(page.body.asString(),id) ?: fail("Mail.ru embed has no matching metadata endpoint")
        val response=client.GET(requestUrl=metadata,overrideHeaders=requestHeaders)
        if(!response.isSuccess) fail("Mail.ru metadata HTTP ${response.statusCode}",response.statusCode)
        val sources=MailruPayload.sources(response.body.asString(),id)
        if(sources.isEmpty()) fail("Mail.ru metadata contains no playable sources")
        val cookie=MailruPayload.cookie(response.headers)
        val mediaHeaders=requestHeaders.filterKeys { !it.equals("Cookie",true) } +
            if(cookie!=null) mapOf("Cookie" to cookie) else emptyMap()
        return sources.map { (quality,source) -> Video(quality=quality,url=source,headers=mediaHeaders) }
    }
    private fun fail(message:String,code:Int=0):Nothing=throw InvalidServerException(message,Error.EXPECTED_RESPONSE_NOT_FOUND,code)
}

internal object MailruPayload {
    fun metadata(html:String,id:String):String? {
        val values=Regex("""["']metadataUrl["']\s*:\s*["']([^"']+)["']""").findAll(html)
        return values.map { it.groupValues[1].replace("\\/","/") }.map { value ->
            when { value.startsWith("//") -> "https:$value";value.startsWith("/") -> "https://my.mail.ru$value";else -> value }
        }.firstOrNull { runCatching { val u=Url(it);u.protocol.name=="https" && u.host=="my.mail.ru" && u.encodedPath=="/+/video/meta/$id" }.getOrDefault(false) }
    }
    fun sources(body:String,id:String):List<Pair<String?,String>> {
        val root=runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return emptyList()
        val meta=root["meta"] as? JsonObject ?: return emptyList()
        if((meta["id"] as? JsonPrimitive)?.contentOrNull!=id) return emptyList()
        return (root["videos"] as? JsonArray).orEmpty().mapNotNull { element ->
            val item=element as? JsonObject ?: return@mapNotNull null
            val raw=(item["url"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
            val source=if(raw.startsWith("//")) "https:$raw" else raw
            val u=runCatching { Url(source) }.getOrNull() ?: return@mapNotNull null
            if(u.protocol.name!="https" || !u.host.endsWith(".my.mail.ru") || source.any(Char::isWhitespace)) return@mapNotNull null
            (item["key"] as? JsonPrimitive)?.contentOrNull to source
        }.distinctBy { it.second }
    }
    fun cookie(headers:Map<String,String>):String? {
        val value=headers.entries.firstOrNull { it.key.equals("Set-Cookie",true) }?.value ?: return null
        return Regex("""(?:^|[,;]\s*)video_key=([^;,\s]+)""").find(value)?.groupValues?.get(1)?.let { "video_key=$it" }
    }
}
