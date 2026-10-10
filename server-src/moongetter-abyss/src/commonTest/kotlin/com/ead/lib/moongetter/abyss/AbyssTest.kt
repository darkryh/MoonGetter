package com.ead.lib.moongetter.abyss

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.io.encoding.Base64
import kotlin.test.*

class AbyssTest {
    @Test fun recognizesProviderAliasesAndRejectsPlaceholderUrls() {
        assertEquals("vu9jynYrzV",AbyssPayload.slug("https://argtesa.top/?v=vu9jynYrzV"))
        assertEquals("vu9jynYrzV",AbyssPayload.slug("https://player.abyss.to/vu9jynYrzV"))
        assertNull(AbyssPayload.slug("https://storage.googleapis.com/mediastorage/123/fake.mp4#mp4/chunk/1"))
        assertNull(AbyssPayload.slug("https://argtesa.top.evil.test/?v=vu9jynYrzV"))
        assertNull(AbyssPayload.slug("https://argtesa.top/?v=short"))
    }
    @Test fun decodesBrowserLatin1PayloadAndDecryptsMetadata(): Unit = runBlocking {
        val plain="""{"mp4":{"sources":[]}}"""
        val encrypted=abyssDecrypt("123:video123:456",plain.encodeToByteArray())
        val data=buildJsonObject {
            put("user_id",123);put("slug","video123");put("md5_id",456)
            put("media",encrypted.map { (it.toInt() and 255).toChar() }.joinToString(""))
        }
        val json=data.toString()
        val encoded=Base64.Default.encode(ByteArray(json.length) { json[it].code.toByte() })
        val decoded=AbyssPayload.data("const datas = '$encoded';","video123")!!
        assertEquals(Json.parseToJsonElement(plain),AbyssPayload.media(decoded))
        assertNull(AbyssPayload.data("const datas = '$encoded';","other123"))
    }
    @Test fun parsesRangesAndRejectsInvalidBounds() {
        assertEquals(AbyssRange(0,99),AbyssRange.parse(null,100))
        assertEquals(AbyssRange(90,99),AbyssRange.parse("bytes=-10",100))
        assertEquals(AbyssRange(10,99),AbyssRange.parse("bytes=10-200",100))
        assertNull(AbyssRange.parse("bytes=100-",100))
        assertNull(AbyssRange.parse("bytes=10-9",100))
        assertNull(AbyssRange.parse("bytes=0-1,5-6",100))
    }
    @Test fun acceptsOnlyValidatedSplitMediaOrigins() {
        fun media(host:String)=Json.parseToJsonElement("""{"mp4":{"sources":[{"codec":"h264","url":"$host","path":"a/b/c/01234567890123456789012345678.131072.3","size":131072,"partSize":65536,"label":"480p"}]}}""").jsonObject
        assertEquals(1,AbyssPayload.sources(media("https://media.sssrr.org")).size)
        assertTrue(AbyssPayload.sources(media("https://media.sssrr.org.evil.test")).isEmpty())
        assertTrue(AbyssPayload.sources(media("http://media.sssrr.org")).isEmpty())
    }
}
