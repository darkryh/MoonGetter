package com.ead.lib.moongetter.xtwitter

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

class XTwitterTest {

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

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""<td><a href='video-url.mp4'>Download</a></td>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val xTwitter = XTwitter(context, url, client , hashMap, configData)
        val videos = xTwitter.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "video-url.mp4")
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
        val xTwitter = XTwitter(context, url, client , hashMap, configData)
        xTwitter.onExtract()

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
        val xTwitter = XTwitter(context, url, client , hashMap, configData)
        xTwitter.onExtract()

        //then
        Unit
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is successful but body isn't expected, throws InvalidServerException`() = runBlocking {

        //given
        val url = server.url("# case match and no url is found").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""<td><a href='#'>Download</a></td>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val xTwitter = XTwitter(context, url, client ,hashMap, configData)
        xTwitter.onExtract()

        //then
        Unit
    }

    @After
    fun tearDown() {
        server.shutdown()
    }
}