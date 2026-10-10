package com.ead.lib.moongetter.client.cookie.java.net

import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class JavaNetCookieManagementTest {
    @Test
    fun sessionCookieRoundTripsAndRespectsScope(): Unit = runBlocking {
        val storage = JavaNetCookieManagement()
        val origin = Url("https://files.example/download/page")
        storage.addCookie(origin, Cookie("session", "a%2Fb", CookieEncoding.RAW, path = "/download", secure = true))
        val stored = storage.get(Url("https://files.example/download/next"))
        assertEquals("a%2Fb", stored.single { it.name == "session" }.value)
        assertTrue(storage.get(Url("https://other.example/download/next")).isEmpty())
        assertTrue(storage.get(Url("https://files.example/unrelated")).isEmpty())
        assertTrue(storage.get(Url("http://files.example/download/next")).isEmpty())
    }

    @Test
    fun rejectsCookieForUnrelatedDomain(): Unit = runBlocking {
        val storage = JavaNetCookieManagement()
        storage.addCookie(Url("https://files.example/"), Cookie("session", "value", domain = "other.example", path = "/"))
        assertTrue(storage.get(Url("https://other.example/")).isEmpty())
    }
}
