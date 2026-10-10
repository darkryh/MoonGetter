package com.ead.lib.moongetter.rpmstream

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.rpmstream.factory.RpmstreamFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class RpmstreamTest {
    private val embed = "https://pelisplus.rpmstream.live/#abc123"
    private val source = "https://pelisplus.rpmstream.live/v4/pl/cdn.example/meb/abc123/master.1.m3u8?k=signed%2Bkey&kx=2000000000"
    private val fixture = "6d6ebd25bbcc2794896b8441d923ec6e27b81cbf526bf0722e980b8d161d4aab871bd5ad42e93621bbf1dd819746c04b0bd71d98796993c6379a3dcd9c9e81b8962823c10f49067abfcfcd1ee7ddbbd0383fab9584b5d9f8eff12ec99013a0ef69c8a51a8c657048548b37d4d49889367a11974461f5ac3e79402daeac3eb32004fe31d71c8d1adbaaf5a1f655131ad555f015d69d5591e95382d9cdb4fe1994af1e3fb6da35f97ee7e5d54cf72c1586"

    @Test fun portableFactoryAndFragmentParsing() {
        assertTrue(Regex(RpmstreamFactory.pattern).matches(embed))
        assertTrue(Regex(RpmstreamFactory.pattern).matches("https://rpmstream.live/#abc123&poster=a"))
        assertEquals("abc123", RpmLink.parse("$embed&poster=a").id)
        for (bad in listOf("https://rpmstream.live.evil/#abc123", "https://evil/rpmstream.live/#abc123", "http://rpmstream.live/#abc123", "https://rpmstream.live/#", "https://rpmstream.live/#abc/123")) {
            assertFalse(Regex(RpmstreamFactory.pattern).matches(bad))
            assertFails { RpmLink.parse(bad) }
        }
    }

    @Test fun independentNodeCipherVector() = runBlocking {
        val payload = RpmPayload.decrypt(fixture)
        assertEquals(source, RpmPayload.nativeSource(payload, RpmLink.parse(embed)))
        assertEquals(payload, RpmPayload.decrypt("\n" + fixture.uppercase() + "\n"))
        for (bad in listOf("not hex", "00", fixture.dropLast(2), "00".repeat(16))) {
            assertFails { RpmPayload.decrypt(bad) }
        }
    }

    @Test fun rejectsWrongVideoHostAndMissingPlaybackTokens() = runBlocking {
        val payload = RpmPayload.decrypt(fixture)
        assertFails { RpmPayload.nativeSource(payload, RpmLink.parse(embed.replace("abc123", "other"))) }
        for (bad in listOf(source.replace("https:", "http:"), source.replace("pelisplus.rpmstream.live", "evil.example"), source.substringBefore('?'), source.replace("k=", "unused="))) {
            assertFails { RpmPayload.nativeSource(JsonObject(payload + ("cfNative" to JsonPrimitive(bad))), RpmLink.parse(embed)) }
        }
    }

    @Test fun completeFlowPreservesSignedQueryAndHeaders() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val client = fake(calls, fixture, "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nvariant.m3u8?k=a&kx=1")
        val videos = Rpmstream(embed, client, hashMapOf("User-Agent" to "test", "referer" to "https://old.example/"), Configuration.Data()).onExtract()
        assertEquals(2, calls.size)
        assertEquals("https://pelisplus.rpmstream.live/api/v1/video?id=abc123&w=1920&h=1080&r=", calls[0].url)
        assertEquals(source, calls[1].url)
        assertEquals(source, videos.single().request.url)
        assertEquals("https://pelisplus.rpmstream.live/", videos.single().request.headers?.get("Referer"))
        assertEquals("test", videos.single().request.headers?.get("User-Agent"))
        assertFalse(videos.single().request.headers.orEmpty().containsKey("referer"))
    }

    @Test fun rejectsHtmlPlaceholderInsteadOfPlaylist() = runBlocking {
        for (placeholder in listOf("<html>Loading</html>", "#EXTM3U\n")) {
            val client = fake(mutableListOf(), fixture, placeholder)
            assertFailsWith<InvalidServerException> { Rpmstream(embed, client, hashMapOf(), Configuration.Data()).onExtract() }
        }
        Unit
    }

    @Test fun reportsApiRateLimitWithoutAttemptingPlayback() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val client = fake(calls, "{\"message\":\"Too many requests\"}", "unused", 429)
        val failure = assertFailsWith<InvalidServerException> {
            Rpmstream(embed, client, hashMapOf(), Configuration.Data()).onExtract()
        }
        assertTrue(failure.message.orEmpty().contains("429"))
        assertEquals(1, calls.size)
    }

    private fun fake(calls: MutableList<Request<*>>, api: String, playlist: String, apiStatus: Int = 200): MoonClient = object : MoonClient {
        override fun initConfigurationData(configData: Configuration.Data) {}
        override suspend fun <T> request(request: Request<T>): Response {
            calls += request
            val text = if (calls.size == 1) api else playlist
            return object : Response {
                override val statusCode = if (calls.size == 1) apiStatus else 200
                override val headers = emptyMap<String, String>()
                override val body = object : ResponseBody { override fun asString() = text }
                override val url = object : Url { override val toString = request.url; override val host = "pelisplus.rpmstream.live" }
            }
        }
    }
}
