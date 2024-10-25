package com.ead.lib.moongetter.server_sites

import org.junit.Before

class FacebookTest {

    /*private lateinit var context: Context
    private lateinit var server : MockWebServer*/

    @Before
    fun setup() {
        /*context = mockk(relaxed = true)
        server = MockWebServer()*/
    }

    /*@Test
    fun `onExtract should add videos when response is successful always returns sd link at first`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                <div>
                    <a id="hdlink" href="https://example.com/video-hd.mp4">HD Download</a>
                </div>
                <div>
                    <a id="sdlink" href="https://example.com/video-sd.mp4">SD Download</a>
                </div>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val facebook = Facebook(context, url)
        facebook.targetUrl = url
        val videos = facebook.onExtract()

        //then
        server.shutdown()
        assert(videos.firstOrNull()?.request?.url == "https://example.com/video-sd.mp4")
    }

    @Test
    fun `onExtract should add videos when response is successful and hd link is found`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                <div>
                    <a id="hdlink" href="https://example.com/video-hd.mp4">HD Download</a>
                </div>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val facebook = Facebook(context, url)
        facebook.targetUrl = url
        val videos = facebook.onExtract()

        //then
        server.shutdown()
        assert(videos.firstOrNull()?.request?.url == "https://example.com/video-hd.mp4")
    }


    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)
            .setBody("""<a id="hdlink" href="https://example.com/video-hd.mp4">HD Download</a>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val facebook = Facebook(context, url)
        facebook.targetUrl = url
        facebook.onExtract()

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
            .setBody("""This is random body""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val facebook = Facebook(context, url)
        facebook.targetUrl = url
        facebook.onExtract()

        //then
        server.shutdown()
    }*/

}