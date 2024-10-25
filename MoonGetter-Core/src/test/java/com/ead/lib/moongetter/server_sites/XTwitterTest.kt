package com.ead.lib.moongetter.server_sites

import org.junit.Before

class XTwitterTest {

    /*private lateinit var context: Context
    private lateinit var server : MockWebServer
*/
    @Before
    fun setup() {
        /*context = mockk(relaxed = true)
        server = MockWebServer()*/
    }

    /*@Test
    fun `onExtract should add videos when response is successful`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""<td><a href='video-url.mp4'>Download</a></td>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val xTwitter = XTwitter(context, url)
        xTwitter.targetUrl = url
        val videos = xTwitter.onExtract()

        //then
        server.shutdown()
        assert(videos.firstOrNull()?.request?.url == "video-url.mp4")
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)
            .setBody("""<td><a href='video-url.mp4'>Download</a></td>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val xTwitter = XTwitter(context, url)
        xTwitter.targetUrl = url
        xTwitter.onExtract()

        //then
        server.shutdown()
    }


    @Test(expected = RuntimeException::class)
    fun `onExtract should add videos when response is successful but body isn't expected with urls with # as answer, throws RuntimeException`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""This is random body""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val xTwitter = XTwitter(context, url)
        xTwitter.targetUrl = url
        xTwitter.onExtract()

        //then
        server.shutdown()
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is successful but body isn't expected, throws InvalidServerException`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""<td><a href='#'>Download</a></td>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val xTwitter = XTwitter(context, url)
        xTwitter.targetUrl = url
        xTwitter.onExtract()

        //then
        server.shutdown()
    }*/
}