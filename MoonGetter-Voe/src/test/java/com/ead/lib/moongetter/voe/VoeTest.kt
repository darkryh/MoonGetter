package com.ead.lib.moongetter.voe

import android.content.Context
import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class VoeTest {
    private lateinit var context: Context
    private lateinit var server : MockWebServer
    private val client = OkHttpClient()

    private val hashMap = hashMapOf<String,String>()
    private val configData = Configuration.Data()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        server = MockWebServer()

        server.start()
    }


    @Test
    fun `onExtract should add videos when response is successful`() = runBlocking {

        //given
        val url = server.url("successful test").toString()
        val url2 = server.url("successful test2").toString()

        var mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                window.location.href = '$url2';
            """.trimIndent())

        server.enqueue(mockResponse)

        mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
            var sources = {
                'hls': 'aHR0cHM6Ly9kZWxpdmVyeS1ub2RlLXdpMTE0eDJicmU0eWx0Z2kudm9lLW5ldHdvcmsubmV0L2VuZ2luZS9obHMyLzAxLzEyMjYxL2JmeWRyNGZtbTdqcF8sbiwudXJsc2V0L21hc3Rlci5tM3U4P3Q9a0x0djdTOWNCT0FuREV0bk52TXZyUm5lWFA0UU5oUFVBWmtlVi1Qal93dyZzPTE3MzE3MTA0MTImZT0xNDQwMCZmPTYxMzA1ODY1Jm5vZGU9ZGVsaXZlcnktbm9kZS13aTExNHgyYnJlNHlsdGdpLnZvZS1uZXR3b3JrLm5ldCZpPTE4MS4xOTkmc3A9MjUwMCZhc249Mjc5NDc=',
                'video_height': 720,
            }; 
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val mp4Upload = Voe(context, url, client , hashMap, configData)

        val videos = mp4Upload.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://delivery-node-wi114x2bre4yltgi.voe-network.net/engine/hls2/01/12261/bfydr4fmm7jp_,n,.urlset/master.m3u8?t=kLtv7S9cBOAnDEtnNvMvrRneXP4QNhPUAZkeV-Pj_ww&s=1731710412&e=14400&f=61305865&node=delivery-node-wi114x2bre4yltgi.voe-network.net&i=181.199&sp=2500&asn=27947")
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {

        //given
        val url = server.url("response is unsuccessful").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)
            .setBody("""<td><a href='video-url.mp4'>Download</a></td>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val mp4Upload = Voe(context, url, client , hashMap, configData)
        mp4Upload.onExtract()

        //then
        Unit
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is successful but body isn't expected with urls with # as answer, throws InvalidServerException`() = runBlocking {

        //given
        val url = server.url("body isn't expected").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""This is random body""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val mp4Upload = Voe(context, url, client ,hashMap, configData)
        mp4Upload.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}