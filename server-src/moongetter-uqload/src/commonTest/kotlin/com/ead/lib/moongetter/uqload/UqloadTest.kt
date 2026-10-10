package com.ead.lib.moongetter.uqload

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.uqload.factory.UqloadFactory
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class UqloadTest {
    private val source = "https://cdn.uqload.vc/master.m3u8?t=signed&x=1"
    private val embed = "https://uqload.co/embed-pcmet1dsyefs.html"
    private val canonical = "https://uqload.vc/embed-pcmet1dsyefs.html?utm_source=mirror_co&"

    @Test fun packedObjectSourcesAndLegacyStrings() {
        val packed = """eval(function(p,a,c,k,e,d){return p}('0("vplayer").1({2:[{3:"4://cdn.uqload.vc/master.m3u8?t=signed&x=one"}]});',5,5,'jwplayer|setup|sources|file|https'.split('|')))"""
        assertEquals(listOf("https://cdn.uqload.vc/master.m3u8?t=signed&x=one"), UqloadPayload.sources(packed))
        assertEquals(listOf(source), UqloadPayload.sources("""sources: ["$source"]"""))
        assertEquals(listOf(source), UqloadPayload.sources("""sources : [
            {file: '$source', type: 'hls'}
        ]"""))
    }

    @Test fun excludesPosterSubtitleAndInvalidSources() {
        val page = """image:'https://cdn.uqload.vc/poster.jpg',tracks:[{file:'https://cdn.uqload.vc/sub.vtt'}],sources:[{file:'$source'},{file:'javascript:bad'},{file:'http://cdn.uqload.vc/video.mp4'}]"""
        assertEquals(listOf(source), UqloadPayload.sources(page))
        assertTrue(UqloadPayload.sources("<html>File unavailable</html>").isEmpty())
    }

    @Test fun recognizesCanonicalQueryAndRejectsSpoofedHosts() {
        assertTrue(Regex(UqloadFactory.pattern).matches(embed))
        assertTrue(Regex(UqloadFactory.pattern).matches(canonical))
        assertEquals(UqloadPayload.embed(embed), UqloadPayload.embed(canonical))
        assertFalse(Regex(UqloadFactory.pattern).matches("https://uqload.co.evil/embed-pcmet1dsyefs.html"))
    }

    @Test fun usesFinalEmbedHeadersAndPreservesSignedHls() = runBlocking {
        val calls = mutableListOf<Request<*>>()
        val client = fake(calls, "sources:[{file:'$source'}]")
        val headers = hashMapOf("User-Agent" to "test", "origin" to "https://wrong.example/")
        val videos = Uqload(embed, client, headers, Configuration.Data()).onExtract()
        assertEquals(2, calls.size)
        assertEquals(source, videos.single().request.url)
        assertEquals(canonical, videos.single().request.headers?.get("Referer"))
        assertEquals("https://uqload.vc", videos.single().request.headers?.get("Origin"))
        assertFalse(videos.single().request.headers.orEmpty().containsKey("origin"))
        assertEquals("https://wrong.example/", headers["origin"])
    }

    @Test fun rejectsBadRedirectsAndPlaylistPlaceholders() = runBlocking {
        for (final in listOf("https://uqload.vc/embed-another.html", "https://evil.example/embed-pcmet1dsyefs.html")) {
            assertFailsWith<InvalidServerException> { Uqload(embed, fake(mutableListOf(), "sources:['$source']", final = final), hashMapOf(), Configuration.Data()).onExtract() }
        }
        assertFailsWith<InvalidServerException> { Uqload(embed, fake(mutableListOf(), "sources:['$source']", playlist = "<html>blocked</html>"), hashMapOf(), Configuration.Data()).onExtract() }
        Unit
    }

    private fun fake(calls: MutableList<Request<*>>, page: String, final: String = canonical, playlist: String = "#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1\nvideo.m3u8"): MoonClient = object : MoonClient {
        override fun initConfigurationData(configData: Configuration.Data) {}
        override suspend fun <T> request(request: Request<T>): Response {
            calls += request
            val location = if (calls.size == 1) final else request.url
            val text = if (calls.size == 1) page else playlist
            return object : Response {
                override val statusCode = 200
                override val headers = emptyMap<String, String>()
                override val url = object : Url { override val toString = location; override val host = "uqload.vc" }
                override val body = object : ResponseBody { override fun asString() = text }
            }
        }
    }
}
