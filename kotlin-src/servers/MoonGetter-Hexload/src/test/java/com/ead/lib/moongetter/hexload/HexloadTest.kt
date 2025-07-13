package com.ead.lib.moongetter.hexload

import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class HexloadTest {
    private val client = OkHttpClient()
    private lateinit var server : MockWebServer

    private val hashMap = hashMapOf<String,String>()
    private val configData = Configuration.Data()

    @Before
    fun setup() {
        server = MockWebServer()

        server.start()
    }


    @Test
    fun `onExtract should add videos when response is successful`() = runBlocking {

        //given
        val url = server.url("successful test").toString()
        val url2 = server.url("successful test 2").toString()

        var mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                 ${'$'}.ajax({
                     type: 'POST',
                     url: 'https://hexload.com/download',
                     data: {
                         op: 'download3',
                         id: "fa3656ue6vep",
                         ajax: '1',
                         method_free: '1',
                         dataType: "json",
                     },
                     success: function (data, textStatus, jqXHR) {

                         if (data.msg === "OK") {
                             var ul = btoa(data.result['url']);
                             b4aa.buy(ul,"aHR0cHM6Ly9oZXRidWlsZDg2MDA5Mi50YWtlcGxjZG4uYXJ0L2kvMDA5OTYvZmEzNjU2dWU2dmVwX3guanBn","https://hexload.com/fa3656ue6vep");
                         }
                     },
                 });
            """.trimIndent())

        server.enqueue(mockResponse)

        mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                 {
                   "status": 200,
                   "result": {
                     "folder": null,
                     "size": 153349994,
                     "content_type": "video/mp4",
                     "file_name": "04-Haiga.mp4",
                     "thumb_url": "https://225517853567582.purpleads.pro/i/00997/jlkfw1mxaaeo_t.jpg",
                     "url": "https://225517853567582.purpleads.pro/d/video_test/video.mp4",
                     "image_url": "https://225517853567582.purpleads.pro/i/00997/jlkfw1mxaaeo.jpg",
                     "md5": "8ytONfXvYvK2d7JoEyOK1w"
                   },
                   "msg": "OK",
                   "server_time": "2024-10-31 15:07:35"
                 }
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        Values.targetUrl2 = url2
        val hexload = Hexload(url, client , hashMap, configData)

        val videos = hexload.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://225517853567582.purpleads.pro/d/video_test/video.mp4")
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
        val hexload = Hexload(url, client , hashMap, configData)
        hexload.onExtract()

        //then
        Unit
    }


    @Test(expected = RuntimeException::class)
    fun `onExtract should add videos when response is successful but body isn't expected with urls with # as answer, throws RuntimeException`() = runBlocking {

        //given
        val url = server.url("body isn't expected").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""This is random body""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val hexload = Hexload(url, client ,hashMap, configData)
        hexload.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}