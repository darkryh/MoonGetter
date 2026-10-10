package com.ead.lib.moongetter.filemoon

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class BysePlaybackTest {
    private val fixture = Json.parseToJsonElement("""{"algorithm":"AES-256-GCM","key_parts":["AAECAwQFBgcICQoLDA0ODw","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","AA","EBESExQVFhcYGRobHB0eHw"],"version":"1","iv":"BwcHBwcHBwcHBwcH","payload":"dEjVMxp9tJG1vynb-3OB6c1-t1Hgg_VpjYFx1WGmU3ZXKJRQ4i-ZvW-f0omnZfdLiD2La_mDbGLwjb6WFMjm0n-HkUL5pEOiLa9eL4msZrSgFjNQxsIuGoJKAjkdVWups9LywaR69v0AGc3yED947eDDrA"}""").jsonObject

    @Test fun authenticatesAndSelectsVersionedKeyParts() = runBlocking {
        val result = BysePlayback.decode(fixture)
        assertEquals("https://example.com/master.m3u8", result.getValue("sources").jsonArray[0].jsonObject.getValue("url").jsonPrimitive.content)
    }

    @Test fun rejectsModifiedPayload() = runBlocking {
        val bad = JsonObject(fixture + ("payload" to JsonPrimitive("A" + fixture.getValue("payload").jsonPrimitive.content.drop(1))))
        assertFails { BysePlayback.decode(bad) }
        Unit
    }
}
