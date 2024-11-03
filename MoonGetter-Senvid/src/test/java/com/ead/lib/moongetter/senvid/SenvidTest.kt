package com.ead.lib.moongetter.senvid

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

class SenvidTest {
    private lateinit var context: Context
    private val client = OkHttpClient()
    private lateinit var server : MockWebServer

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

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""<source src="https://videos2.sendvid.com/b7/fc/k555oewr.mp4?validfrom=1730297370&validto=1730311770&rate=200k&ip=157.100.108.221&hash=CutqIYYGOT76FVxgKBAREd9whjg%3D" type="video/mp4" id="video_source"/>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val senvid = Senvid(context, url, client , hashMap, configData)
        val videos = senvid.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://videos2.sendvid.com/b7/fc/k555oewr.mp4?validfrom=1730297370&validto=1730311770&rate=200k&ip=157.100.108.221&hash=CutqIYYGOT76FVxgKBAREd9whjg%3D")
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {

        //given
        val url = server.url("response is unsuccessful").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val senvid = Senvid(context, url, client , hashMap, configData)
        senvid.onExtract()

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
        val senvid = Senvid(context, url, client , hashMap, configData)
        senvid.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}