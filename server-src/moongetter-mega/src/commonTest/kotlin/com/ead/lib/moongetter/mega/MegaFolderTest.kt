package com.ead.lib.moongetter.mega

import com.ead.lib.moongetter.mega.factory.MegaFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.test.*

class MegaFolderTest {
    private val url = "https://mega.nz/folder/ABCDEFGH#ICEiIyQlJicoKSorLC0uLw"
    private val root = """{"h":"ROOTROOT","p":"OUTSIDE1","t":1}"""
    private fun file(handle: String = "FILE0001", parent: String = "ROOTROOT", video: Boolean = true) =
        """{"h":"$handle","p":"$parent","t":0,"s":${if(video) 256 else 0},"k":"OTHERKEY:unused/ROOTROOT:23zmevE95XqV2SLlMlq_E3cQmKa3jLRcApzxwN3uDxs","a":"${if(video) "wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs" else "uf3yUIEsYOGK_toWIUIwyt44ppAI1tMahu4KPo85eaA"}"}"""
    private fun tree(vararg entries: String) = Json.parseToJsonElement(entries.joinToString(",", "[", "]")).jsonArray

    @Test fun recognizesFolderAndSelectedFileLinks() {
        for (link in listOf(url, "$url/file/FILE0001", "$url/folder/SUBFOLD1", "$url/folder/SUBFOLD1/file/FILE0001")) {
            assertTrue(Regex(MegaFactory.pattern).matches(link))
            assertEquals("ABCDEFGH", MegaFolderLink.parse(link).handle)
        }
        for (bad in listOf(url.replace("mega.nz", "mega.nz.evil"), "$url/file/short", "$url/unknown/FILE0001")) {
            assertFalse(Regex(MegaFactory.pattern).matches(bad))
            assertFails { MegaFolderLink.parse(bad) }
        }
    }

    @Test fun unwrapsIndependentEcbVectorAndAutoSelectsOneVideo() = runBlocking {
        val folder = MegaFolderLink.parse(url)
        val selected = MegaFolder.select(folder, tree(root, file(), file("TEXT0001", video = false)))
        try {
            assertEquals("FILE0001", selected.handle)
            assertEquals("ABCDEFGH", selected.folderHandle)
            assertEquals("fixture.mp4", selected.expectedName)
            assertEquals(256, selected.expectedSize)
            assertContentEquals(ByteArray(32) { it.toByte() }, selected.secret)
        } finally { selected.clear(); folder.secret.fill(0) }
    }

    @Test fun multipleVideosRequireExplicitSelection() = runBlocking {
        val nodes = tree(root, file(), file("FILE0002"))
        val folder = MegaFolderLink.parse(url)
        assertTrue(assertFails { MegaFolder.select(folder, nodes) }.message.orEmpty().contains("2 videos"))
        val selected = MegaFolder.select(MegaFolderLink.parse("$url/file/FILE0002"), nodes)
        try { assertEquals("FILE0002", selected.handle) } finally { selected.clear() }
    }

    @Test fun selectedSubfolderRestrictsEligibleFiles() = runBlocking {
        val sub = """{"h":"SUBFOLD1","p":"ROOTROOT","t":1}"""
        val nodes = tree(root, sub, file(), file("FILE0002", "SUBFOLD1"))
        val selected = MegaFolder.select(MegaFolderLink.parse("$url/folder/SUBFOLD1"), nodes)
        try { assertEquals("FILE0002", selected.handle) } finally { selected.clear() }
        assertFails { MegaFolder.select(MegaFolderLink.parse("$url/folder/SUBFOLD1/file/FILE0001"), nodes) }
        Unit
    }

    @Test fun rejectsMissingNonVideoAndUndecryptableCandidates() = runBlocking {
        assertFails { MegaFolder.select(MegaFolderLink.parse("$url/file/MISSING1"), tree(root, file())) }
        assertFails { MegaFolder.select(MegaFolderLink.parse("$url/file/TEXT0001"), tree(root, file("TEXT0001", video = false))) }
        assertFails { MegaFolder.select(MegaFolderLink.parse(url), tree(root, file(), file("FILE0002").replace("wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs", "invalid"))) }
        assertFails { MegaFolder.select(MegaFolderLink.parse(url), tree(root, file(), file())) }
        Unit
    }

    @Test fun folderTicketsAndRefreshKeepPublicFolderScope() = runBlocking {
        var listings = 0
        var tickets = 0
        var reads = 0
        val http = HttpClient(MockEngine { request ->
            if (request.url.host == "g.api.mega.co.nz") {
                assertEquals("ABCDEFGH", request.url.parameters["n"])
                val body = (request.body as TextContent).text
                if (body.contains("\"a\":\"f\"")) {
                    listings++
                    respond("[{\"f\":${tree(root, file())}}]")
                } else {
                    tickets++
                    assertTrue(body.contains("\"n\":\"FILE0001\""))
                    assertFalse(body.contains("\"p\":"))
                    respond("""[{"s":256,"at":"wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs","g":"https://test.mega.co.nz/ticket"}]""")
                }
            } else { reads++; respond("", if (reads == 1) HttpStatusCode.Forbidden else HttpStatusCode.TooManyRequests) }
        })
        val link = MegaFolder.resolve(url, http)
        val session = MegaSession.createWithClient(link, http)
        try {
            assertEquals(429, assertFailsWith<MegaUpstreamException> { session.read(0, 31) }.localStatus)
            assertEquals(1, listings)
            assertEquals(2, tickets)
        } finally { session.close() }
        assertTrue(link.secret.all { it == 0.toByte() })
    }

    @Test fun rejectsChangedSelectedFileMetadataAndClearsKey() = runBlocking {
        val link = MegaFolder.select(MegaFolderLink.parse(url), tree(root, file()))
        val http = HttpClient(MockEngine {
            respond("""[{"s":257,"at":"wpKQwlnQlVnqJllVhq4k3zBTgKSh6w8g94ZOR0VRWJs","g":"https://test.mega.co.nz/ticket"}]""")
        })
        assertFailsWith<IllegalStateException> { MegaSession.createWithClient(link, http) }
        assertTrue(link.secret.all { it == 0.toByte() })
    }
}
