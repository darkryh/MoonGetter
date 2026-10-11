package com.ead.lib.moongetter.server.bundle

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.resolution.EmbedUrl
import com.ead.lib.moongetter.resolution.ResolutionConfig
import com.ead.lib.moongetter.resolution.ServerResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/** Shared data for the cross-server tests: the real server bundle, the host registry and the page fixtures. */
internal object BundleTestSupport {

    val factories: List<Server.Factory> = serverBundle.toList()

    /** Servers with structured routing data. */
    val described: List<Server.Factory> = factories.filter { it.descriptor != null }

    /** Servers still routed by their historical pattern. */
    val legacy: List<Server.Factory> = factories.filter { it.descriptor == null }

    private val root = File(System.getProperty("moongetter.root") ?: error("moongetter.root system property is not set"))

    /** Registry service names that differ from the factory's serverName. */
    private val serverNameOf = mapOf("Vidhide" to "Vihide")

    /** A path the service accepts, used to turn hostnames into routable URLs. */
    val samplePaths: Map<String, List<String>> = mapOf(
        "Abyss" to listOf("/K8R6OOjS7", "/?v=vu9jynYrzV"),
        "Doodstream" to listOf("/e/abcd1234"),
        "Filemoon" to listOf("/e/abcd1234", "/abcd1234"),
        "Lulustream" to listOf("/e/abcd1234"),
        "RPMStream" to listOf("/#abc123", "/#abc123&poster=x"),
        "Streamtape" to listOf("/e/abcd1234"),
        "Streamwish" to listOf("/e/abcd1234"),
        "Vihide" to listOf("/v/abcd1234", "/embed/abcd1234"),
        "Mixdrop" to listOf("/e/abcd1234"),
        "Voe" to listOf("/e/abcd1234", "/d/abcd1234"),
    )

    data class RegistryHost(val host: String, val routed: Boolean, val status: String, val reason: String)
    data class RegistryService(val serverName: String, val hosts: List<RegistryHost>, val families: List<String>, val subdomainsOf: List<String>) {
        val routed get() = hosts.filter { it.routed }
        val excluded get() = hosts.filterNot { it.routed }
    }

    val registry: List<RegistryService> by lazy {
        val json = Json.parseToJsonElement(File(root, "tools/domain-registry/registry.json").readText()).jsonObject
        json["services"]!!.jsonArray.map { element ->
            val service = element.jsonObject
            val name = service.string("service")
            RegistryService(
                serverName = serverNameOf[name] ?: name,
                hosts = service["hosts"]!!.jsonArray.map {
                    val host = it.jsonObject
                    RegistryHost(host.string("host"), host["routed"]!!.jsonPrimitive.boolean, host.string("status"), host.string("reason"))
                },
                families = service["families"]!!.jsonArray.map { it.jsonObject.string("pattern") },
                subdomainsOf = service["subdomainsOf"]!!.jsonArray.map { it.jsonPrimitive.content },
            )
        }
    }

    fun factory(serverName: String): Server.Factory = factories.single { it.serverName == serverName }

    /** Every routable sample URL for every routed host of [service]. */
    fun sampleUrls(service: RegistryService): List<String> =
        service.routed.flatMap { host -> samplePaths.getValue(service.serverName).map { "https://${host.host}$it" } }

    fun resolver(config: ResolutionConfig = ResolutionConfig(learnedHosts = null)) = ServerResolver(factories, config)

    /** Servers whose static routing claims [url]: descriptor matches, plus legacy patterns as MoonGetter applies them. */
    fun claimants(url: String): List<String> {
        val parsed = EmbedUrl.parse(url)
        val byDescriptor = described.filter { parsed != null && it.descriptor!!.match(parsed) != null }.map { it.serverName }
        val byPattern = legacy.filter { Regex(it.pattern).containsMatchIn(url) }.map { it.serverName }
        return byDescriptor + byPattern
    }

    data class Expectation(
        val outcome: String,
        val stage: String?,
        val server: String?,
        val reason: String?,
        val url: String?,
        val suspects: List<String>?,
        val candidates: List<String>?,
    )

    data class Fixture(val file: String, val truth: String?, val path: String, val status: Int, val role: String, val expect: Expectation) {
        val html: String by lazy { resource("fixtures/$file") }
        /** Served from an unlisted host so the fallback has to identify it. */
        val url: String get() = "https://unlisted-${file.substringBefore('.').replace('_', '-')}.xyz$path"
    }

    val fixtures: List<Fixture> by lazy {
        Json.parseToJsonElement(resource("fixtures/fixtures.json")).jsonObject["fixtures"]!!.jsonArray.map { element ->
            val fixture = element.jsonObject
            val expect = fixture["expect"]!!.jsonObject
            Fixture(
                file = fixture.string("file"),
                truth = fixture["truth"]?.jsonPrimitive?.takeIf { it.isString }?.content,
                path = fixture.string("path"),
                status = fixture["status"]!!.jsonPrimitive.content.toInt(),
                role = fixture.string("role"),
                expect = Expectation(
                    outcome = expect.string("outcome"),
                    stage = expect.optional("stage"),
                    server = expect.optional("server"),
                    reason = expect.optional("reason"),
                    url = expect.optional("url"),
                    suspects = expect["suspects"]?.jsonArray?.map { it.jsonPrimitive.content },
                    candidates = expect["candidates"]?.jsonArray?.map { it.jsonPrimitive.content },
                ),
            )
        }
    }

    fun resource(path: String): String =
        BundleTestSupport::class.java.classLoader.getResource(path)?.readText() ?: error("Missing test resource $path")

    private fun JsonObject.string(key: String) = this[key]!!.jsonPrimitive.content
    private fun JsonObject.optional(key: String) = this[key]?.jsonPrimitive?.content
}

/**
 * Serves fixture pages by URL and records requests. Any other URL answers an empty 404, the way a
 * dead link would, so a test never reaches the network.
 */
internal class PageClient(private val pages: Map<String, Pair<Int, String>>) : MoonClient {
    val requests = mutableListOf<String>()

    override fun initConfigurationData(configData: Configuration.Data) = Unit

    override suspend fun <T> request(request: Request<T>): Response {
        requests += request.url
        val (status, body) = pages[request.url] ?: (404 to "")
        return object : Response {
            override val statusCode = status
            override val headers = emptyMap<String, String>()
            override val body = object : ResponseBody { override fun asString() = body }
            override val url = object : Url {
                override val toString = request.url
                override val host = EmbedUrl.parse(request.url)?.host.orEmpty()
            }
        }
    }
}
