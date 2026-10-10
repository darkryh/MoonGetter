package com.ead.lib.moongetter.onefichier

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class OnefichierTest {
    private val pageUrl = "https://1fichier.com/?npic4u16rb5qucp7ug9w"
    private val source = "https://a-1.1fichier.com/signed%2Ftoken/video?auth=a%2Bb"
    private val page = """var ct = 0; <form id="f1" method="post" action="$pageUrl">
        <span class="tier-name">4482_1_SUB.mp4</span><input type="hidden" name="token" value="keep" /></form>"""
    @Test fun matchesFileUrlsAndRestrictsFormActions() {
        assertTrue(Regex(OnefichierPage.PATTERN).matches(pageUrl))
        assertFalse(Regex(OnefichierPage.PATTERN).matches("https://1fichier.com.evil/?id"))
        assertFalse(Regex(OnefichierPage.PATTERN).matches("https://1fichier.com/?id&url=evil"))
        assertEquals(60, OnefichierPage.form(page.replace("ct = 0", "ct = 60"), pageUrl)?.waitSeconds)
        assertNull(OnefichierPage.form(page.replace("ct = 0", "ct = 61"), pageUrl))
        assertNull(OnefichierPage.form(page.replace(pageUrl, "https://evil.test/?id"), pageUrl))
        assertNull(OnefichierPage.form(page.replace("npic4u16rb5qucp7ug9w", "other"), pageUrl))
    }
    @Test fun readsOnlyProviderDownloadAnchors() {
        assertEquals(source, OnefichierPage.source("""<a class="ok btn-general" href="$source">Download</a>"""))
        assertNull(OnefichierPage.source("""<a class="ok" href="https://evil.test/video">Download</a>"""))
        assertFalse(OnefichierPage.validSource("https://a.1fichier.com.evil/video"))
        assertFalse(OnefichierPage.validSource("http://a.1fichier.com/video"))
        assertFalse(OnefichierPage.validSource("https://user@a.1fichier.com/video"))
    }
    @Test fun resolvesNormalFormAndRejectsHtmlMedia() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val result = Onefichier(pageUrl, fake(calls), hashMapOf(), Configuration.Data()).onExtract().single().request
        assertEquals(source, result.url)
        assertEquals(3, calls.size)
        assertFalse(calls.last().isResponseBodyNeeded)
        assertEquals("POST", calls[1].method.toString())
        assertEquals(pageUrl, result.headers?.get("Referer"))
        assertFailsWith<InvalidServerException> {
            Onefichier(pageUrl, fake(mutableListOf(), type = "text/html"), hashMapOf(), Configuration.Data()).onExtract()
        }
        Unit
    }
    @Test fun reportsGuestCapacityWithoutInventingASource() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val error = assertFailsWith<InvalidServerException> {
            Onefichier(pageUrl, fake(calls, ticket = "all free guest slots are currently in use"), hashMapOf(), Configuration.Data()).onExtract()
        }
        assertTrue(error.message.orEmpty().contains("guest slots"))
        assertEquals(2, calls.size)
        assertNotNull(OnefichierPage.restriction("You must wait 30 minutes"))
        Unit
    }
    private fun fake(calls: MutableList<Request<*>>, ticket: String = """<a class="ok" href="$source">Download</a>""", type: String = "video/mp4") = object : MoonClient {
        override fun initConfigurationData(configData: Configuration.Data) {}
        override suspend fun <T> request(request: Request<T>): Response {
            calls += request
            val index = calls.size
            return object : Response {
                override val statusCode = 200
                override val headers = mapOf("Content-Type" to if (index == 3) type else "text/html")
                override val url = object : Url { override val host = "1fichier.com"; override val toString = if (index == 3) source else pageUrl }
                override val body = object : ResponseBody { override fun asString() = if (index == 1) page else ticket }
            }
        }
    }
}
