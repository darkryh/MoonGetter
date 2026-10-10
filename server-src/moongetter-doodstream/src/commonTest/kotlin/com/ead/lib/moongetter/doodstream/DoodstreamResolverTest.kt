package com.ead.lib.moongetter.doodstream

import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import kotlin.test.*

class DoodstreamResolverTest {
    private val old = "https://dsvplay.com/e/j0jlmfjfalnj"
    private val current = "https://playmogo.com/e/j0jlmfjfalnj"
    private val player = """$.get('/pass_md5/a/b',function(){});function makePlay(){return random+"?token=a&expiry="+Date.now();}"""

    @Test fun resolvesClosedLegacyAliasThroughCanonicalRedirect() = runBlocking {
        val attempts = mutableListOf<String>()
        val resolved = DoodstreamResolver.resolve(old, mapOf("User-Agent" to "test")) { target, headers ->
            attempts += target
            assertEquals("test", headers["User-Agent"])
            if (attempts.size == 1) throw IOException("connection closed")
            assertEquals("https://doodstream.com", headers["Origin"])
            response(current, player)
        }
        assertEquals(listOf(old, "https://doodstream.com/e/j0jlmfjfalnj"), attempts)
        assertEquals(current, resolved.url.toString)
    }

    @Test fun reportsChallengeAndBoundsCanonicalAttempts() = runBlocking {
        var attempts = 0
        val error = assertFailsWith<InvalidServerException> {
            DoodstreamResolver.resolve("https://doodstream.com/e/j0jlmfjfalnj", emptyMap()) { _, _ ->
                attempts++
                response(current, "<title>Just a moment...</title>/cdn-cgi/challenge-platform/main.js", 403)
            }
        }
        assertEquals(1, attempts)
        assertEquals(403, error.code)
        assertTrue(error.message.orEmpty().contains("verification is required"))
    }

    @Test fun rejectsRedirectToAnotherVideoAndDoesNotCacheFailure() = runBlocking {
        assertFailsWith<InvalidServerException> {
            DoodstreamResolver.resolve(old, emptyMap()) { _, _ -> response("https://playmogo.com/e/other", player) }
        }
        val recovered = DoodstreamResolver.resolve(old, emptyMap()) { _, _ -> response(current, player) }
        assertEquals(current, recovered.url.toString)
    }

    @Test fun propagatesCancellationWithoutFallback() = runBlocking {
        var attempts = 0
        assertFailsWith<CancellationException> {
            DoodstreamResolver.resolve(old, emptyMap()) { _, _ -> attempts++; throw CancellationException() }
        }
        assertEquals(1, attempts)
    }

    private fun response(target: String, text: String, status: Int = 200) = object : Response {
        override val statusCode = status
        override val headers = emptyMap<String, String>()
        override val body = object : ResponseBody { override fun asString() = text }
        override val url = object : Url {
            override val toString = target
            override val host = "playmogo.com"
        }
    }
}
