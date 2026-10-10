package com.ead.lib.moongetter.fireload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class FireloadTest {
    private val id = "5cab2fd19d300d84"
    private val pageUrl = "https://www.fireload.com/$id/So_Big!_(1932)_(HDrip_480p).mp4"
    private val ticket = "$pageUrl?pt=one%2Fuse%3D"
    private val source = "https://srv1.fireload.com/$id/So_Big!_(1932)_(HDrip_480p).mp4?token=media%2Bsigned"
    private val page = """window.Fl = {
        "dlink": "$ticket", "dwait": "0", "dtext": "Download File"
    }"""

    @Test fun parsesConfigurationAndValidatesScope() {
        assertEquals(FireloadPayload.Ticket(ticket, 0), FireloadPayload.ticket(page, id))
        assertNull(FireloadPayload.ticket(page.replace(id, "0000000000000000"), id))
        assertNull(FireloadPayload.ticket(page.replace("www.fireload.com", "evil.example"), id))
        assertNull(FireloadPayload.ticket(page.replace("\"0\"", "\"-1\""), id))
        assertNull(FireloadPayload.ticket(page.replace("\"0\"", "\"61\""), id))
        assertNull(FireloadPayload.ticket(page.replace("?pt=one%2Fuse%3D", ""), id))
        assertEquals(3, FireloadPayload.ticket(page.replace("\"0\"", "\"3\""), id)?.waitSeconds)
        assertFalse(FireloadPayload.validMedia(source.replace("srv1.fireload.com", "srv1.fireload.com.evil"), id))
        assertFalse(FireloadPayload.validMedia(source, "0000000000000000"))
    }

    @Test fun factoryAndCookieAreProviderSpecific() {
        assertEquals(id, FireloadPayload.fileId(pageUrl))
        assertNull(FireloadPayload.fileId("https://www.fireload.com/folder/example"))
        assertNull(FireloadPayload.fileId(pageUrl.replace("fireload.com", "fireload.com.evil")))
        assertEquals("filehosting=guest123", FireloadPayload.cookie(mapOf("set-cookie" to "filehosting=guest123; path=/; HttpOnly")))
    }

    @Test fun forwardsGuestCookieOnlyToTicketAndReturnsResolvedSource() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val original = hashMapOf("Cookie" to "unrelated=old", "origin" to "old")
        val server = Fireload(pageUrl, fake(calls), original, Configuration.Data())
        assertFalse(server.isDeprecated)
        val result = server.onExtract().single().request
        assertEquals(source, result.url)
        assertEquals(ticket, calls.last().url)
        assertFalse(calls.last().isResponseBodyNeeded)
        assertEquals("filehosting=guest123", calls.last().headers["Cookie"])
        assertFalse(result.headers.orEmpty().keys.any { it.equals("Cookie", true) })
        assertEquals(pageUrl, result.headers?.get("Referer"))
        assertEquals("unrelated=old", original["Cookie"])
        assertEquals(2, calls.size)
    }

    @Test fun rejectsUnresolvedTicketHtmlAndForeignRedirects() = runBlocking {
        for ((target, type) in listOf(ticket to "text/html", source to "text/html", source.replace(id, "0000000000000000") to "video/mp4", "https://evil.example/video.mp4" to "video/mp4")) {
            assertFailsWith<InvalidServerException> { Fireload(pageUrl, fake(mutableListOf(), target, type), hashMapOf(), Configuration.Data()).onExtract() }
        }
        Unit
    }

    private fun fake(calls: MutableList<Request<*>>, final: String = source, type: String = "video/mp4") = object : MoonClient {
        override fun initConfigurationData(configData: Configuration.Data) {}
        override suspend fun <T> request(request: Request<T>): Response {
            calls += request
            return object : Response {
                override val statusCode = 200
                override val headers = if (calls.size == 1) mapOf("Set-Cookie" to "filehosting=guest123; path=/") else mapOf("content-type" to type)
                override val url = object : Url { override val host = "www.fireload.com"; override val toString = if (calls.size == 1) pageUrl else final }
                override val body = object : ResponseBody { override fun asString() = page }
            }
        }
    }
}
