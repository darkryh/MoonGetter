package com.ead.lib.moongetter.mediafire

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

class MediafireTest {
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
                    <a class="input popsok"
                   aria-label="Download file"
                   href="https://download2393.mediafire.com/yzu5nyjhq88g5ZesNvW9GdNsx3RTWDHvXstjE39C6Fv5X0Rw_CLuqKE2DLlBClFjF8zOKUMXPozYPsZio4tb4sNFJt4J5MO_12uiCbMtz-jP9-YppWgpz6ZPJowTArx6SH9IPfQh3BlEGwLVEPXtoapmt4J5yfyBBJ8Q1LtsvSYfE_I/w1pcadajnxxakyg/4-youkai.mp4"           id="downloadButton"
                   rel="nofollow">
                        Download (157.56MB)
                </a>
                <a class="starting" href="#"><span>Your download is starting...</span></a>
            <a class="retry" href="https://www.mediafire.com/download_repair.php?qkey=w1pcadajnxxakyg&amp;dkey=yzu5nyjhq88&amp;template=59&amp;origin=click_button">
                <span class="notranslate">Download Started. <em>Repair your download</em></span>
            </a>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val mediafire = Mediafire(url, client , hashMap, configData)

        val videos = mediafire.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://download2393.mediafire.com/yzu5nyjhq88g5ZesNvW9GdNsx3RTWDHvXstjE39C6Fv5X0Rw_CLuqKE2DLlBClFjF8zOKUMXPozYPsZio4tb4sNFJt4J5MO_12uiCbMtz-jP9-YppWgpz6ZPJowTArx6SH9IPfQh3BlEGwLVEPXtoapmt4J5yfyBBJ8Q1LtsvSYfE_I/w1pcadajnxxakyg/4-youkai.mp4")
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
        val mediafire = Mediafire(url, client , hashMap, configData)
        mediafire.onExtract()

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
        val mediafire = Mediafire(url, client , hashMap, configData)
        mediafire.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}