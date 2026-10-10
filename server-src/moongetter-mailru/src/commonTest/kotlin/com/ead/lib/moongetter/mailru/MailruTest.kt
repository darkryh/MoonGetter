package com.ead.lib.moongetter.mailru
import com.ead.lib.moongetter.mailru.factory.MailruFactory
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class MailruTest {
    private val embed="https://my.mail.ru/video/embed/123"
    private val metadata="""{"meta":{"id":"123"},"videos":[{"key":"720p","url":"//cdn64.my.mail.ru/hv/file.mp4?token=a&b=2"},{"key":"360p","url":"https://cdn64.my.mail.ru/v/file.mp4"}]}"""
    @Test fun factoryAndMetadataRejectSpoofs() {
        assertTrue(Regex(MailruFactory.pattern).matches("$embed#user#42"))
        assertFalse(Regex(MailruFactory.pattern).matches("https://my.mail.ru.evil/video/embed/123"))
        assertEquals("https://my.mail.ru/+/video/meta/123",MailruPayload.metadata("""{"metadataUrl":"//my.mail.ru/+/video/meta/123"}""","123"))
        assertNull(MailruPayload.metadata("""{"metadataUrl":"https://evil/+/video/meta/123"}""","123"))
        assertNull(MailruPayload.metadata("""{"metadataUrl":"/+/video/meta/999"}""","123"))
    }
    @Test fun preservesQualityAndSignedQuery() {
        val sources=MailruPayload.sources(metadata,"123")
        assertEquals(2,sources.size)
        assertEquals("720p",sources[0].first)
        assertEquals("https://cdn64.my.mail.ru/hv/file.mp4?token=a&b=2",sources[0].second)
        assertTrue(MailruPayload.sources(metadata,"999").isEmpty())
        assertTrue(MailruPayload.sources("bad JSON","123").isEmpty())
    }
    @Test fun isolatesPlaybackCookie() {
        assertEquals("video_key=abc",MailruPayload.cookie(mapOf("set-cookie" to "other=x; Path=/, video_key=abc; Domain=.my.mail.ru; Secure")))
        assertNull(MailruPayload.cookie(emptyMap()))
    }
    @Test fun completeTwoRequestFlow() = runBlocking {
        val requests=mutableListOf<Request<*>>()
        val client=object:MoonClient {
            override fun initConfigurationData(configData:Configuration.Data) {}
            override suspend fun <T> request(request:Request<T>):Response {
                requests+=request
                val text=if(requests.size==1) """{"metadataUrl":"/+/video/meta/123"}""" else metadata
                return object:Response {
                    override val statusCode=200
                    override val headers=mapOf("Set-Cookie" to "video_key=abc; Secure","Server" to "ignored")
                    override val body=object:ResponseBody {override fun asString()=text}
                    override val url=object:Url {override val toString=request.url;override val host="my.mail.ru"}
                }
            }
        }
        val videos=Mailru("$embed#user#42",client,hashMapOf("User-Agent" to "test"),Configuration.Data()).onExtract()
        assertEquals(2,requests.size)
        assertEquals(embed,requests[0].url)
        assertEquals("https://my.mail.ru/+/video/meta/123",requests[1].url)
        assertEquals("video_key=abc",videos.first().request.headers?.get("Cookie"))
        assertEquals(embed,videos.first().request.headers?.get("Referer"))
        assertFalse(videos.first().request.headers.orEmpty().containsKey("Set-Cookie"))
    }
}
