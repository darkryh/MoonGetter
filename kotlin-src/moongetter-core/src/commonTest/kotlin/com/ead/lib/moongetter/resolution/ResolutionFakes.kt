package com.ead.lib.moongetter.resolution

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video

/** A scripted response: what the client returns for one requested URL. */
internal data class FakePage(
    val body: String,
    val status: Int = 200,
    /** The URL after HTTP redirects; defaults to the requested URL. */
    val finalUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

/** A [MoonClient] answering from a URL map and recording every request. Unknown URLs throw. */
internal class FakeClient(private val pages: Map<String, FakePage> = emptyMap()) : MoonClient {
    val requests = mutableListOf<Request<*>>()
    val requestedUrls: List<String> get() = requests.map { it.url }

    override fun initConfigurationData(configData: Configuration.Data) = Unit

    override suspend fun <T> request(request: Request<T>): Response {
        requests += request
        val page = pages[request.url] ?: throw IllegalStateException("Unexpected request to ${request.url}")
        return page.toResponse(request.url)
    }
}

internal fun FakePage.toResponse(requested: String): Response = object : Response {
    override val statusCode = status
    override val headers = this@toResponse.headers
    override val body = object : ResponseBody { override fun asString() = this@toResponse.body }
    override val url = object : Url {
        override val toString = finalUrl ?: requested
        override val host = EmbedUrl.parse(finalUrl ?: requested)?.host.orEmpty()
    }
}

/** A configurable factory whose server runs [extract]. */
internal class FakeFactory(
    override val serverName: String,
    override val descriptor: ServerDescriptor?,
    legacyPattern: String = "",
    private val extract: suspend (url: String, client: MoonClient) -> List<Video> = { url, _ -> listOf(Video(url = "$url#video")) },
) : Server.Factory {
    override val pattern: String = descriptor?.pattern ?: legacyPattern
    val createdWith = mutableListOf<Pair<String, HashMap<String, String>>>()

    override fun create(url: String, headers: HashMap<String, String>, configData: Configuration.Data, client: MoonClient): Server {
        createdWith += url to headers
        return object : Server(url, client, headers, configData) {
            override suspend fun onExtract(): List<Video> = extract(url, client)
        }
    }
}

internal fun unique(id: String, pattern: String, scope: SignalScope = SignalScope.BODY) =
    Signal.pattern(id, SignalKind.UNIQUE, scope, pattern)

internal fun supporting(id: String, pattern: String, scope: SignalScope = SignalScope.BODY) =
    Signal.pattern(id, SignalKind.SUPPORTING, scope, pattern)

internal fun familyMarker(id: String, pattern: String, scope: SignalScope = SignalScope.BODY) =
    Signal.pattern(id, SignalKind.FAMILY, scope, pattern)

/** A synthetic family whose pages share `family-a` and `family-b` markers. */
internal val TestFamily = ServerFamily(
    id = "test-family",
    signals = listOf(familyMarker("family.a", "family-a"), familyMarker("family.b", "family-b"), familyMarker("family.c", "family-c")),
    minimumHits = 2,
)

internal val EmbedPath = PathRule("""/e/([A-Za-z0-9]+)/?(?:[?#]\S*)?""")

/** A descriptor-backed fake listing [hosts] with optional fingerprint signals and family. */
internal fun fakeFactory(
    name: String,
    hosts: List<String>,
    signals: List<Signal> = emptyList(),
    family: ServerFamily? = null,
    paths: List<PathRule> = listOf(EmbedPath),
    extract: suspend (url: String, client: MoonClient) -> List<Video> = { url, _ -> listOf(Video(url = "$url#video")) },
) = FakeFactory(
    serverName = name,
    descriptor = ServerDescriptor(
        hosts = HostSet(exact = hosts),
        paths = paths,
        fingerprint = signals.takeIf { it.isNotEmpty() }?.let { Fingerprint(it) },
        family = family,
    ),
    extract = extract,
)

internal fun page(body: String, url: String = "https://unknown-mirror.xyz/e/abc123", status: Int = 200) =
    PageSnapshot(url, url, status, emptyMap(), body)
