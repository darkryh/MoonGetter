package com.ead.lib.moongetter.pixeldrain


import com.ead.lib.moongetter.models.Configuration
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.utils.Values
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class PixeldrainTest {
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

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
            player.src({
            type: "video/mp4",
            src: "https://a4.mp4upload.com:183/d/xkxw6mujz3b4quuoo6rbwy2plgvhwrqub6eiysvetztpjstheehj4kayo7kwjvgvh74f4v5c/video.mp4"           
           });
            if ("https://a4.mp4upload.com/i/02683/a1oxgwvtpj04.jpg") {
             player.poster("https://a4.mp4upload.com/i/02683/a1oxgwvtpj04.jpg");
            }
          });  
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val mp4Upload = Pixeldrain(url, client ,hashMap, configData)

        val videos = mp4Upload.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://a4.mp4upload.com:183/d/xkxw6mujz3b4quuoo6rbwy2plgvhwrqub6eiysvetztpjstheehj4kayo7kwjvgvh74f4v5c/video.mp4")
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
        val mp4Upload = Pixeldrain(url, client , hashMap, configData)
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
        val mp4Upload = Pixeldrain(url, client , hashMap, configData)
        mp4Upload.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}