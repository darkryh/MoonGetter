package com.ead.lib.moongetter.server.bundle

import com.ead.lib.moongetter.resolution.EmbedUrl
import com.ead.lib.moongetter.resolution.PageClassifier
import com.ead.lib.moongetter.resolution.PageKind
import com.ead.lib.moongetter.resolution.PageSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * Re-checks every registry host against the live web and reports drift. Opt-in only
 * (`MOONGETTER_DOMAIN_DRIFT=1`): results depend on the network and the vantage point, so this test
 * never fails a build. It writes `build/reports/domain-drift/report.json` and `summary.md` for review.
 */
class DomainDriftLiveTest {

    private data class Observation(val service: String, val host: String, val recorded: String, val routed: Boolean, val observed: String, val detail: String)

    @Test fun reportDomainDrift() {
        if (System.getenv("MOONGETTER_DOMAIN_DRIFT") != "1") return
        val hosts = BundleTestSupport.registry.flatMap { service -> service.hosts.map { service.serverName to it } }
        val pool = Executors.newFixedThreadPool(16)
        val observations = try {
            pool.invokeAll(hosts.map { (service, host) -> Callable { observe(service, host) } }, 15, TimeUnit.MINUTES).map { it.get() }
        } finally {
            pool.shutdownNow()
        }

        val drift = observations.filter { drifted(it) }
        val out = File("build/reports/domain-drift").apply { mkdirs() }
        out.resolve("report.json").writeText(buildJsonObject {
            put("checked", observations.size)
            put("drifted", drift.size)
            put("observations", JsonArray(observations.map { o ->
                buildJsonObject {
                    put("service", o.service); put("host", o.host); put("recorded", o.recorded); put("routed", o.routed)
                    put("observed", o.observed); put("detail", o.detail); put("drift", JsonPrimitive(drifted(o)))
                }
            }))
        }.toString())
        out.resolve("summary.md").writeText(buildString {
            appendLine("# Domain drift: ${drift.size} of ${observations.size} hosts changed")
            appendLine()
            appendLine("| Service | Host | Routed | Recorded | Observed | Detail |")
            appendLine("|---|---|---|---|---|---|")
            drift.sortedWith(compareBy({ it.service }, { it.host })).forEach {
                appendLine("| ${it.service} | ${it.host} | ${it.routed} | ${it.recorded} | ${it.observed} | ${it.detail.replace("|", "/")} |")
            }
        })
        println(out.resolve("summary.md").readText())
    }

    /** A routed host that stopped working, or an excluded host that came back. */
    private fun drifted(o: Observation): Boolean = when {
        o.routed -> o.observed in setOf("dead", "parked")
        else -> o.observed == "live" && o.recorded in setOf("dead", "parked")
    }

    private fun observe(service: String, host: BundleTestSupport.RegistryHost): Observation {
        val (observed, detail) = try {
            val connection = URI("https://${host.host}/").toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (MoonGetter domain drift check)")
            try {
                val status = connection.responseCode
                when (status) {
                    in 300..399 -> {
                        val location = connection.getHeaderField("Location").orEmpty()
                        val target = EmbedUrl.parse("https://${host.host}/")?.resolve(location)?.host ?: location
                        "redirects" to "HTTP $status -> $target"
                    }
                    else -> {
                        val body = (if (status < 400) connection.inputStream else connection.errorStream)
                            ?.bufferedReader()?.use { reader -> CharArray(200_000).let { buffer -> String(buffer, 0, maxOf(0, reader.read(buffer))) } }
                            .orEmpty()
                        when (PageClassifier.classify(PageSnapshot("https://${host.host}/", "https://${host.host}/", status, emptyMap(), body))) {
                            PageKind.PARKED -> "parked" to "HTTP $status parked page"
                            PageKind.CHALLENGE -> "unverified" to "HTTP $status challenge"
                            else -> "live" to "HTTP $status"
                        }
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: UnknownHostException) {
            "dead" to "DNS resolution failed"
        } catch (error: Exception) {
            "unverified" to "${error::class.simpleName}: ${error.message.orEmpty().take(80)}"
        }
        return Observation(service, host.host, host.status, host.routed, observed, detail)
    }
}
