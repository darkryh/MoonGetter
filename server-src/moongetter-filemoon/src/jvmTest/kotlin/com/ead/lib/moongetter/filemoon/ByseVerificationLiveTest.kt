package com.ead.lib.moongetter.filemoon

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ByseVerificationLiveTest {
    @Test fun verificationUsesHttpAndPortableWork() = runBlocking {
        val target = System.getenv("MOONGETTER_BYSE_VERIFY_TEST_URL") ?: return@runBlocking
        val url = Url(target)
        require(url.host == "bysekoze.com")
        val code = url.segments.last()
        val origin = "${url.protocol.name}://${url.host}"
        val endpoint = "$origin/api/videos/$code/embed"
        val client = HttpClient(CIO) {
            install(HttpCookies)
            install(HttpTimeout) { requestTimeoutMillis = 60_000 }
        }
        try {
            suspend fun post(path: String, body: String, token: String? = null) = client.post("$endpoint/$path") {
                header(HttpHeaders.UserAgent, "Mozilla/5.0")
                header(HttpHeaders.Origin, origin)
                header(HttpHeaders.Referrer, target)
                token?.let { header("X-Captcha-Token", it) }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            val challenge = post("captcha", "{}")
            assertEquals(HttpStatusCode.OK, challenge.status)
            val data = Json.parseToJsonElement(challenge.bodyAsText()).jsonObject
            val solution = withTimeout(60_000) {
                ByseProofOfWork.solve(data.getValue("pow_nonce").jsonPrimitive.content,
                    data.getValue("pow_difficulty").jsonPrimitive.int)
            }
            assertTrue(solution != null, "No solution within the work budget")
            val verified = post("captcha/verify", buildJsonObject {
                put("pow_token", data.getValue("pow_token"))
                put("solution", solution)
            }.toString())
            assertEquals(HttpStatusCode.OK, verified.status)
            val result = Json.parseToJsonElement(verified.bodyAsText()).jsonObject
            assertEquals("ok", result.getValue("status").jsonPrimitive.content)
            val token = result.getValue("token").jsonPrimitive.content
            assertTrue(token.isNotEmpty())
            val playback = post("playback", """{"fingerprint":{"token":"","viewer_id":"","device_id":""}}""", token)
            println("Byse HTTP verification accepted; playback HTTP ${playback.status.value}. This does not establish extraction or playback success.")
            assertEquals(HttpStatusCode.OK, playback.status)
            val encrypted = Json.parseToJsonElement(playback.bodyAsText()).jsonObject.getValue("playback").jsonObject
            assertTrue(BysePlayback.decode(encrypted).getValue("sources").jsonArray.isNotEmpty())
        } finally {
            client.close()
        }
    }
}
