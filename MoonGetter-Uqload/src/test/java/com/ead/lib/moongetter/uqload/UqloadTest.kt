package com.ead.lib.moongetter.uqload

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

class UqloadTest {
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
            .setBody("""sources: ["https://m110.uqload.net/3rfk3nvqajw2q4drdlg7fn5ykptktjb5horboalppkzuymwz73ceuk5q5kya/v.mp4"]""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val uqload = Uqload(url, client , hashMap, configData)
        val videos = uqload.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://m110.uqload.net/3rfk3nvqajw2q4drdlg7fn5ykptktjb5horboalppkzuymwz73ceuk5q5kya/v.mp4")
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
        val uqload = Uqload(url, client ,hashMap, configData)
        uqload.onExtract()

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
        val uqload = Uqload(url, client ,hashMap, configData)
        uqload.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}