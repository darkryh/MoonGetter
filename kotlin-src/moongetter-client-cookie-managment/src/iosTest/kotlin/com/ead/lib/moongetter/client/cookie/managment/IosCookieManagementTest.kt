package com.ead.lib.moongetter.client.cookie.managment

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class IosCookieManagementTest {
    @Test fun matchesDomainPathAndSecureTransport() = runBlocking {
        val manager = IosCookieManagement()
        try {
            manager.addCookie(Url("https://video.example.com/media/embed"),
                Cookie("session", "value", domain = ".example.com", path = "/media", secure = true))
            assertEquals("value", manager.get(Url("https://cdn.example.com/media/video")).single().value)
            assertTrue(manager.get(Url("http://cdn.example.com/media/video")).isEmpty())
            assertTrue(manager.get(Url("https://cdn.example.com/other")).isEmpty())
            assertTrue(manager.get(Url("https://evil.example/media/video")).isEmpty())
        } finally { manager.close() }
    }

    @Test fun replacesCookiesAndKeepsInstancesIndependent() = runBlocking {
        val first = IosCookieManagement()
        val second = IosCookieManagement()
        val url = Url("https://video.example.com/media/embed")
        try {
            first.addCookie(url, Cookie("session", "old"))
            first.addCookie(url, Cookie("session", "new"))
            assertEquals(listOf("new"), first.get(url).map { it.value })
            assertTrue(second.get(url).isEmpty())
        } finally { first.close(); second.close() }
    }
}
