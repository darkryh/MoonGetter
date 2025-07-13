package com.ead.lib.moongetter.googledrive

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

class GoogleDriveTest {

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
                <form id="download-form" action="https://drive.usercontent.google.com/download" method="get">
                <input type="hidden" name="id" value="0B-bHILyvaZQQNXo5cWpXMjRhZ2M">
                <input type="hidden" name="export" value="download">
                <input type="hidden" name="confirm" value="t">
                <input type="hidden" name="uuid" value="d758aa46-4906-4f6f-8382-7a128f5703ce">
            </form>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        Values.targetUrl = url
        val googleDrive = GoogleDrive(url, client , hashMap, configData)

        val videos = googleDrive.onExtract()

        //then
        assert(videos.firstOrNull()?.request?.url == "https://drive.usercontent.google.com/download?id=0B-bHILyvaZQQNXo5cWpXMjRhZ2M&export=download&authuser=0&confirm=t&uuid=d758aa46-4906-4f6f-8382-7a128f5703ce")
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
        val googleDrive = GoogleDrive(url, client , hashMap, configData)
        googleDrive.onExtract()

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
        val googleDrive = GoogleDrive(url, client , hashMap, configData)
        googleDrive.onExtract()

        //then
        Unit
    }


    @After
    fun tearDown() {
        server.shutdown()
    }
}