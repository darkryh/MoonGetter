package com.ead.lib.moongetter.server_sites

import org.junit.Before

class OkruTest {

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

        val mockJsonObject = mockk<JSONObject>(relaxed = true)

        every { mockJsonObject.getJSONObject("flashvars") } returns mockk {
            every { getJSONArray("videos") } returns mockk {
                every { length() } returns 1
                every { getJSONObject(0) } returns mockk {
                    every { getString("name") } returns "sd"
                    every { getString("url") } returns "https://example.com/video.mp4"
                }
            }
        }

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(200)
            .setBody("""
                data-options="{&quot;flashvars&quot;:{&quot;videos&quot;:[{&quot;name&quot;:&quot;sd&quot;,&quot;url&quot;:&quot;https://example.com/video.mp4&quot;}]}}"
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val okru = Okru(context, url)
        val videos = okru.onExtract()

        println(videos)
        //then
        server.shutdown()
        assert(videos.size == 1)
        assert(videos[0].quality == "480p")
        assert(videos[0].request.url == "https://example.com/video.mp4")
    }

    @Test(expected = InvalidServerException::class)
    fun `onExtract should add videos when response is unsuccessful`() = runBlocking {
        server.start()
        //given

        val url = server.url("").toString()

        val mockResponse = MockResponse()
            .setResponseCode(400)
            .setBody("""
                <div class="video-container" data-options='{"flashvars": {"metadata": "{\"videos\":[{\"name\":\"mobile\",\"url\":\"https://cdn.okru.com/video/mobile.mp4\"},{\"name\":\"low\",\"url\":\"https://cdn.okru.com/video/360p.mp4\"},{\"name\":\"hd\",\"url\":\"https://cdn.okru.com/video/720p.mp4\"}]}"}}'></div>
            """.trimIndent())
        server.enqueue(mockResponse)

        //when
        val okru = Okru(context, url)
        okru.onExtract()

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
            .setBody("""
                <div class="video-container" data-options='{"flashvars": {"metadata": "{\"videos\":[{\"name\":\"mobile\",\"url\":\"https://cdn.okru.com/video/mobile.mp4\"},{\"name\":\"low\",\"url\":\"https://cdn.okru.com/video/360p.mp4\"},{\"name\":\"hd\",\"url\":\"https://cdn.okru.com/video/720p.mp4\"}]}"}}'></div>
            """.trimIndent())

        server.enqueue(mockResponse)

        //when
        val okru = Okru(context, url)
        okru.onExtract()

        //then
        server.shutdown()
    }*/
}