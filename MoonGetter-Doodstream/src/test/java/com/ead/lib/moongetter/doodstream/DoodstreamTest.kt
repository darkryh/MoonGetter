package com.ead.lib.moongetter.doodstream

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

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class DoodstreamTest {
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
    fun `onExtract should add videos when response is successful always returns sd link at first`() = runBlocking {

        //given
        val url = server.url("successful test in case of multiple links always returns sd link at first").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                    <a id="hdlink" href="https://example.com/video-hd.mp4">HD Download</a>
                    <a id="sdlink" href="https://example.com/video-sd.mp4">SD Download</a>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val doodstream = Doodstream(url, client , hashMap, configData)
        Values.targetUrl = url
        val videos = doodstream.onExtract()

        //then
        server.shutdown()
        assert(videos.firstOrNull()?.request?.url == "https://example.com/video-sd.mp4")
    }

    @Test
    fun `onExtract should add videos when response is successful and hd link is found`() = runBlocking {

        //given

        val url = server.url("response is successful and hd link is found").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                    <a id="hdlink" href="https://example.com/video-hd.mp4">HD Download</a>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val doodstream = Doodstream(url, client , hashMap, configData)
        Values.targetUrl = url
        val videos = doodstream.onExtract()

        //then
        server.shutdown()
        assert(videos.firstOrNull()?.request?.url == "https://example.com/video-hd.mp4")
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {

        //given

        val url = server.url("response is unsuccessful").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)

        server.enqueue(mockResponse)

        //when
        val doodstream = Doodstream(url, client ,hashMap, configData)
        Values.targetUrl = url
        doodstream.onExtract()

        //then
        server.shutdown()
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is successful but body isn't expected, throws InvalidServerException`() = runBlocking {

        //given

        val url = server.url("response is successful but body isn't expected").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""This is random body""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val facebook = Doodstream(url, client ,hashMap, configData)
        Values.targetUrl = url
        facebook.onExtract()

        //then
        server.shutdown()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}