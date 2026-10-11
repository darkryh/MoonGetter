package com.ead.lib.moongetter.resolution

import com.ead.lib.moongetter.MoonGetter
import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.models.error.Error
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** End to end through [MoonGetter]: a fallback answer is only accepted once an extractor returns videos. */
class FallbackExtractionTest {

    private val previous = MoonGetter.instance
    private val unknown = "https://unknown-mirror.xyz/e/abc123"
    private val events = mutableListOf<ResolutionEvent>()

    @AfterTest fun restore() {
        MoonGetter.instance = previous
    }

    private fun start(
        client: MoonClient,
        factories: List<Server.Factory>,
        config: ResolutionConfig = ResolutionConfig(fallbackEnabled = true, listener = { events += it }),
    ) = MoonGetter.start(Factory.Builder().apply {
        this.client = client
        engine = Engine.Builder().onCore(factories.toTypedArray()).build()
        setResolution(config)
    })

    private fun failing(name: String, family: ServerFamily? = TestFamily, attempts: MutableList<String>) =
        fakeFactory(name, listOf("${name.lowercase()}.com"), family = family) { url, _ ->
            attempts += name
            throw InvalidServerException("$name cannot read $url", Error.EXPECTED_RESPONSE_NOT_FOUND)
        }

    private fun working(name: String, family: ServerFamily? = TestFamily, attempts: MutableList<String>) =
        fakeFactory(name, listOf("${name.lowercase()}.com"), family = family) { url, client ->
            attempts += name
            client.request<String>(com.ead.lib.moongetter.client.request.Request(com.ead.lib.moongetter.client.request.HttpMethod.GET, url))
            listOf(Video(url = "https://media.example/$name.m3u8"))
        }

    @Test fun familyProbingTriesMembersUntilOneExtracts(): Unit = runBlocking {
        val attempts = mutableListOf<String>()
        val client = FakeClient(mapOf(unknown to FakePage("family-a family-b")))
        start(client, listOf(failing("X1", attempts = attempts), working("X2", attempts = attempts), working("X3", attempts = attempts)))

        val server = MoonGetter.get(unknown)
        assertEquals(listOf("X1", "X2"), attempts)
        assertEquals("https://media.example/X2.m3u8", server.videos.single().request.url)
        // The fallback page was replayed to each extractor: one network request in total.
        assertEquals(listOf(unknown), client.requestedUrls)

        val confirmed = assertIs<ResolutionEvent.UnknownHostConfirmed>(events.single())
        assertEquals("X2", confirmed.serverName)
        assertEquals(ResolutionStage.FAMILY_PROBE, confirmed.stage)
        assertEquals("unknown-mirror.xyz", confirmed.host)
    }

    @Test fun confirmedHostsAreLearnedAndRoutedWithoutAnotherProbe(): Unit = runBlocking {
        val attempts = mutableListOf<String>()
        val store = InMemoryLearnedHostStore()
        val client = FakeClient(mapOf(unknown to FakePage("family-a family-b")))
        start(client, listOf(failing("X1", attempts = attempts), working("X2", attempts = attempts)),
            ResolutionConfig(fallbackEnabled = true, learnedHosts = store))

        MoonGetter.get(unknown)
        assertEquals("X2", store.serverFor("unknown-mirror.xyz"))
        assertEquals("X2", MoonGetter.identifier("https://unknown-mirror.xyz/e/other99"))

        val next = assertIs<Resolution.Resolved>(MoonGetter.resolve("https://unknown-mirror.xyz/e/abc123"))
        assertEquals(ResolutionStage.LEARNED_HOST, next.stage)
        attempts.clear()
        MoonGetter.get(unknown)
        assertEquals(listOf("X2"), attempts, "a learned host goes straight to its server")
    }

    @Test fun aLearnedHostThatStopsWorkingIsForgotten(): Unit = runBlocking {
        val store = InMemoryLearnedHostStore().apply { remember("unknown-mirror.xyz", "X1") }
        val attempts = mutableListOf<String>()
        start(FakeClient(), listOf(failing("X1", attempts = attempts)),
            ResolutionConfig(fallbackEnabled = true, learnedHosts = store, listener = { events += it }))

        assertFailsWith<InvalidServerException> { MoonGetter.get(unknown) }
        assertNull(store.serverFor("unknown-mirror.xyz"))
        assertIs<ResolutionEvent.LearnedHostRejected>(events.single())
    }

    @Test fun whenEveryCandidateFailsTheFirstFailureIsReported(): Unit = runBlocking {
        val attempts = mutableListOf<String>()
        start(FakeClient(mapOf(unknown to FakePage("family-a family-b"))),
            listOf(failing("X1", attempts = attempts), failing("X2", attempts = attempts)))
        val error = assertFailsWith<InvalidServerException> { MoonGetter.get(unknown) }
        assertTrue(error.message!!.startsWith("X1"))
        assertEquals(listOf("X1", "X2"), attempts)
        assertTrue(events.none { it is ResolutionEvent.UnknownHostConfirmed })
    }

    @Test fun anEmptyExtractionDoesNotConfirmAFallbackServer(): Unit = runBlocking {
        val empty = fakeFactory("Empty", listOf("empty.com"), listOf(unique("empty.unique", "empty-unique"))) { _, _ -> emptyList() }
        val store = InMemoryLearnedHostStore()
        start(FakeClient(mapOf(unknown to FakePage("empty-unique"))), listOf(empty), ResolutionConfig(fallbackEnabled = true, learnedHosts = store))
        assertFailsWith<InvalidServerException> { MoonGetter.get(unknown) }
        assertTrue(store.snapshot().isEmpty())
    }

    @Test fun eachCandidateReceivesItsOwnHeaders(): Unit = runBlocking {
        val seen = mutableListOf<HashMap<String, String>>()
        fun mutating(name: String, succeed: Boolean) = object : Server.Factory {
            override val serverName = name
            override val descriptor = ServerDescriptor(HostSet(exact = listOf("${name.lowercase()}.com")), listOf(EmbedPath), family = TestFamily)
            override val pattern = descriptor.pattern
            override fun create(url: String, headers: HashMap<String, String>, configData: com.ead.lib.moongetter.client.models.Configuration.Data, client: MoonClient) =
                object : Server(url, client, headers, configData) {
                    override suspend fun onExtract(): List<Video> {
                        seen += HashMap(headers)
                        headers["Referer"] = "set-by-$name"
                        if (!succeed) throw InvalidServerException("no", Error.EXPECTED_RESPONSE_NOT_FOUND)
                        return listOf(Video(url = "https://media.example/v.m3u8"))
                    }
                }
        }
        start(FakeClient(mapOf(unknown to FakePage("family-a family-b"))), listOf(mutating("X1", false), mutating("X2", true)))
        MoonGetter.get(unknown)
        assertEquals(listOf(null, null), seen.map { it["Referer"] }, "a failed candidate's header changes must not leak")
    }

    @Test fun ambiguousPagesFailWithADedicatedError(): Unit = runBlocking {
        val alpha = fakeFactory("Alpha", listOf("alpha.com"), listOf(unique("a", "alpha-unique")))
        val beta = fakeFactory("Beta", listOf("beta.com"), listOf(unique("b", "beta-unique")))
        start(FakeClient(mapOf(unknown to FakePage("alpha-unique beta-unique"))), listOf(alpha, beta))
        val error = assertFailsWith<InvalidServerException> { MoonGetter.get(unknown) }
        assertEquals(Error.AMBIGUOUS_URL_ENTRY, error.error)
        assertEquals(UnresolvedReason.AMBIGUOUS, (events.single() as ResolutionEvent.Unresolved).reason)
        assertNull(MoonGetter.getOrNull(unknown))
    }

    @Test fun knownHostsKeepHistoricalBehaviour(): Unit = runBlocking {
        val alpha = fakeFactory("Alpha", listOf("alpha.com")) { _, _ -> throw InvalidServerException("original", Error.RESOURCE_TAKEN_DOWN) }
        val client = FakeClient()
        start(client, listOf(alpha))
        val error = assertFailsWith<InvalidServerException> { MoonGetter.get("https://alpha.com/e/abc123") }
        assertEquals(Error.RESOURCE_TAKEN_DOWN, error.error)
        assertTrue(client.requests.isEmpty())
        assertTrue(events.isEmpty(), "known hosts are not fallback events")
    }

    @Test fun defaultConfigurationRunsTheFallbackForUnknownHostsOnly() = runBlocking {
        val alpha = fakeFactory("Alpha", listOf("alpha.com"), listOf(unique("a", "alpha-unique")))
        val client = FakeClient(mapOf(unknown to FakePage("alpha-unique")))
        MoonGetter.start(Factory.Builder().apply {
            this.client = client
            engine = Engine.Builder().onCore(arrayOf(alpha)).build()
        })
        assertEquals("https://alpha.com/e/abc123#video", MoonGetter.get("https://alpha.com/e/abc123").videos.single().request.url)
        assertTrue(client.requests.isEmpty(), "a known host is extracted without a fallback request")
        assertEquals("$unknown#video", MoonGetter.get(unknown).videos.single().request.url)
        assertEquals(listOf(unknown), client.requestedUrls)
    }

    @Test fun disabledFallbackKeepsTheUnknownUrlError(): Unit = runBlocking {
        val client = FakeClient(mapOf(unknown to FakePage("alpha-unique")))
        start(client, listOf(fakeFactory("Alpha", listOf("alpha.com"), listOf(unique("a", "alpha-unique")))), ResolutionConfig(fallbackEnabled = false))
        val error = assertFailsWith<InvalidServerException> { MoonGetter.get(unknown) }
        assertEquals(Error.UNKNOWN_URL_ENTRY, error.error)
        assertTrue(client.requests.isEmpty())
        assertNull(MoonGetter.identifier(unknown))
    }

    @Test fun aNewClientReplacesThePreviousOne(): Unit = runBlocking {
        val first = FakeClient(mapOf(unknown to FakePage("family-a family-b")))
        val attempts = mutableListOf<String>()
        start(first, listOf(working("X1", attempts = attempts)))
        MoonGetter.get(unknown)
        val second = FakeClient(mapOf(unknown to FakePage("family-a family-b")))
        start(second, listOf(working("X1", attempts = attempts)))
        MoonGetter.get(unknown)
        assertEquals(1, first.requests.size)
        assertEquals(1, second.requests.size)
    }
}
