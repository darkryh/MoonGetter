package com.ead.lib.moongetter.gofile
import com.ead.lib.moongetter.gofile.factory.GofileFactory
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import com.ead.lib.moongetter.vidguard.factory.util.executeRunnableScript
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class GofileTest {
    @Test fun completesFreeSessionAndSeparatesMediaCredentials() = runBlocking {
        val requests=mutableListOf<com.ead.lib.moongetter.client.request.Request<*>>()
        val guest="a".repeat(32)
        val client=object:com.ead.lib.moongetter.client.MoonClient {
            override fun initConfigurationData(configData:com.ead.lib.moongetter.client.models.Configuration.Data) {}
            override suspend fun <T> request(request:com.ead.lib.moongetter.client.request.Request<T>):com.ead.lib.moongetter.client.response.Response {
                requests+=request
                val text=when(requests.size) {
                    1 -> "function generateWT(token){if(navigator.userAgent!=='test-agent'||navigator.language!=='en-US')throw Error('context');return 'b'.repeat(64);}"
                    2 -> """{"status":"ok","data":{"token":"$guest"}}"""
                    else -> """{"status":"ok","data":{"type":"file","name":"clip.mp4","link":"https://store1.gofile.io/download/a/clip.mp4"}}"""
                }
                return object:com.ead.lib.moongetter.client.response.Response {
                    override val statusCode=200
                    override val headers=emptyMap<String,String>()
                    override val body=object:com.ead.lib.moongetter.client.response.body.ResponseBody {override fun asString()=text}
                    override val url=object:com.ead.lib.moongetter.client.response.url.Url {override val toString=request.url;override val host="gofile.io"}
                }
            }
        }
        val video=Gofile("https://gofile.io/d/zcIJuTK3",client,hashMapOf("User-Agent" to "test-agent"),com.ead.lib.moongetter.client.models.Configuration.Data()).onExtract().single()
        assertEquals(3,requests.size)
        assertNull(requests[1].body)
        assertEquals("Bearer $guest",requests[2].headers["Authorization"])
        assertEquals("b".repeat(64),requests[2].headers["X-Website-Token"])
        assertEquals("accountToken=$guest",video.request.headers?.get("Cookie"))
        assertFalse(video.request.headers.orEmpty().containsKey("Authorization"))
        assertFalse(video.request.headers.orEmpty().containsKey("X-Website-Token"))
    }

    @Test fun recognizesFolderOnly() {
        assertTrue(Regex(GofileFactory.pattern).matches("https://gofile.io/d/zcIJuTK3"))
        assertFalse(Regex(GofileFactory.pattern).matches("https://gofile.io.evil/d/zcIJuTK3"))
    }
    @Test fun selectsVideoAndRejectsUntrustedLinks() {
        val data = Json.parseToJsonElement("""{"type":"folder","children":{"a":{"type":"file","name":"clip.mp4","link":"https://store1.gofile.io/download/a/clip.mp4"},"b":{"type":"file","mimetype":"video/mp4","link":"https://evil.example/a"},"c":{"type":"file","name":"notes.txt","link":"https://store1.gofile.io/a"}}}""").jsonObject
        assertEquals(listOf("https://store1.gofile.io/download/a/clip.mp4"), GofilePayload.sources(data))
    }
    @Test fun reportsGatesWithoutCallingThemDeleted() {
        for (status in listOf("error-notPremium", "error-rateLimit")) {
            val e=assertFailsWith<InvalidServerException> { GofilePayload.data("""{"status":"$status","data":{}}""",401) }
            assertFalse(e.message.orEmpty().contains("deleted"))
        }
        assertFailsWith<InvalidServerException> { GofilePayload.data("""{"status":"ok","data":{"canAccess":false}}""",200) }
    }
    @Test fun executesWebsiteContextWithoutWebView() = runBlocking {
        val output=executeRunnableScript("var navigator={userAgent:'test',language:'en-US'};var globalThis=this;function generateWT(token){return token+navigator.language;}var svg={token:generateWT('guest')};")
        assertEquals("guesten-US",Json.parseToJsonElement(output).jsonObject["token"]!!.jsonPrimitive.content)
    }
}
