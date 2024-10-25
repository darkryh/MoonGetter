package com.ead.lib.moongetter.server_sites

import org.junit.Before

class AnonfilesTest {

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
            .setBody("""<a href="https://cdn-123.anonfiles.com/file.mp4">Download file</a>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val anonfiles = Anonfiles(context, url)
        val videos = anonfiles.onExtract()

        //then
        server.shutdown()
        assert(videos.firstOrNull()?.request?.url == "https://cdn-123.anonfiles.com/file.mp4")
    }

    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)
            .setBody("""<a href="https://cdn-123.anonfiles.com/abc123/file.mp4">Download file</a>""".trimIndent())

        server.enqueue(mockResponse)

        //when
        val anonfiles = Anonfiles(context, url)
        anonfiles.onExtract()

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
        val anonfiles = Anonfiles(context, url)
        anonfiles.onExtract()

        //then
        server.shutdown()
    }*/
}