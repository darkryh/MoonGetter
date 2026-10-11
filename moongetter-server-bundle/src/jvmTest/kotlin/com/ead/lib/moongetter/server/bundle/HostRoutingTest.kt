package com.ead.lib.moongetter.server.bundle

import com.ead.lib.moongetter.resolution.EmbedUrl
import com.ead.lib.moongetter.resolution.Resolution
import com.ead.lib.moongetter.resolution.ResolutionStage
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.claimants
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.factory
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.registry
import com.ead.lib.moongetter.server.bundle.BundleTestSupport.sampleUrls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Static routing over the real server bundle and the host registry: every listed host reaches exactly
 * its own server, and nothing else does.
 */
class HostRoutingTest {

    @Test fun registryCoversEveryDescribedServerAndMatchesTheGeneratedLists() {
        val names = registry.map { it.serverName }.toSet()
        assertEquals(BundleTestSupport.described.map { it.serverName }.toSet(), names,
            "every server with a descriptor must have a registry entry and the reverse")
        registry.forEach { service ->
            val hosts = factory(service.serverName).descriptor!!.hosts
            assertEquals(service.routed.map { it.host }.toSet(), hosts.exact,
                "${service.serverName}: generated host list is stale, run ./gradlew generateHostRegistry")
            assertEquals(service.families, hosts.families, service.serverName)
            assertEquals(service.subdomainsOf.toSet(), hosts.subdomainsOf, service.serverName)
        }
    }

    @Test fun registryHostsAreValidAndUnique() {
        val owners = mutableMapOf<String, String>()
        registry.forEach { service ->
            service.hosts.forEach { host ->
                assertEquals(host.host, EmbedUrl.parse("https://${host.host}/")?.host, "invalid hostname ${host.host}")
                assertTrue(host.reason.isNotBlank(), "${host.host} needs a reason")
                if (host.routed) owners.put(host.host, service.serverName)?.let { fail("${host.host} routed by both $it and ${service.serverName}") }
            }
        }
    }

    @Test fun everyRoutedHostReachesExactlyItsServer() {
        val resolver = BundleTestSupport.resolver()
        val failures = registry.flatMap { service ->
            sampleUrls(service).mapNotNull { url ->
                val claimed = claimants(url)
                val resolution = resolver.resolveStatic(url)
                val resolvedTo = (resolution as? Resolution.Resolved)?.takeIf { it.stage == ResolutionStage.KNOWN_HOST }?.primary?.factory?.serverName
                if (claimed == listOf(service.serverName) && resolvedTo == service.serverName) null
                else "$url -> claimed by $claimed, resolved to $resolvedTo"
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test fun excludedHostsAreNeverClaimedByAnotherServer() {
        // Dead, parked and sink hosts may still fall inside their own service's bounded family; that is
        // acceptable. They must never reach a different server.
        val failures = registry.flatMap { service ->
            service.excluded.flatMap { host ->
                BundleTestSupport.samplePaths.getValue(service.serverName).mapNotNull { path ->
                    val url = "https://${host.host}$path"
                    claimants(url).filter { it != service.serverName }.takeIf { it.isNotEmpty() }?.let { "$url (${host.reason}) -> $it" }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test fun excludedHostsOutsideFamiliesAreNotRoutedStatically() {
        val resolver = BundleTestSupport.resolver()
        registry.forEach { service ->
            val hosts = factory(service.serverName).descriptor!!.hosts
            service.excluded.filterNot { hosts.contains(it.host) }.forEach { host ->
                val url = "https://${host.host}${BundleTestSupport.samplePaths.getValue(service.serverName).first()}"
                assertTrue(resolver.resolveStatic(url) is Resolution.Unresolved, "$url (${host.status}) should be left to the fallback")
            }
        }
    }

    @Test fun lookalikeAndSmuggledHostsReachNoServer() {
        val failures = registry.flatMap { service ->
            val path = BundleTestSupport.samplePaths.getValue(service.serverName).first()
            service.routed.flatMap { routed ->
                val host = routed.host
                listOf(
                    "https://$host.evil-domain.xyz$path",
                    "https://x$host$path".takeIf { !service.subdomainsOf.any { apex -> "x$host".endsWith(".$apex") } },
                    "https://evil-domain.xyz$path?u=https://$host$path",
                    "https://evil-domain.xyz/https://$host$path",
                    "https://$host@evil-domain.xyz$path",
                    "https://evil-domain.xyz#https://$host$path",
                    "https://${host.replace('.', '-')}.xyz$path",
                ).filterNotNull().mapNotNull { url ->
                    claimants(url).takeIf { it.isNotEmpty() }?.let { "$url -> $it" }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.take(50).joinToString("\n"))
    }

    @Test fun familyPatternsNeverMatchAnotherServicesHosts() {
        val failures = registry.flatMap { service ->
            val families = service.families.map { Regex(it) }
            registry.filter { it !== service }.flatMap { other ->
                other.hosts.map { it.host }.filter { host -> families.any { it.matches(host.removePrefix("www.")) } }
                    .map { "${service.serverName} family claims ${other.serverName} host $it" }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test fun legacyPatternsDoNotClaimDescribedHosts() {
        // A descriptor match wins over legacy patterns, which would hide a collision; check them directly.
        val failures = registry.flatMap { service ->
            sampleUrls(service).flatMap { url ->
                BundleTestSupport.legacy.filter { Regex(it.pattern).containsMatchIn(url) }.map { "${it.serverName} legacy pattern claims $url" }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    @Test fun derivedPatternsAgreeWithDescriptors() {
        val disagreements = registry.flatMap { service ->
            val descriptor = factory(service.serverName).descriptor!!
            val regex = Regex(factory(service.serverName).pattern)
            val urls = sampleUrls(service) + sampleUrls(service).flatMap { url ->
                listOf(url.replace("https://", "http://"), url.replace("https://", "https://www."), "$url/extra", url.replace("https://", "https://evil.") + ".evil")
            }
            urls.filter { (descriptor.match(it) != null) != regex.matches(it) }.map { "${service.serverName}: $it" }
        }
        assertTrue(disagreements.isEmpty(), disagreements.joinToString("\n"))
    }

    @Test fun previouslySupportedUrlsStillRoute() {
        mapOf(
            "https://voe.sx/e/abc123" to "Voe",
            "https://teresapoliticallearn.com/e/yo1xkhom61ev" to "Voe",
            "https://dsvplay.com/e/j0jlmfjfalnj" to "Doodstream",
            "https://www.dsvplay.com/e/j0jlmfjfalnj/?autoplay=1#play" to "Doodstream",
            "https://playmogo.com/e/j0jlmfjfalnj" to "Doodstream",
            "https://doodstream.com/e/abcdef1234" to "Doodstream",
            "https://d-s.io/e/abcd1234" to "Doodstream",
            "https://filemoon.sx/abcd1234" to "Filemoon",
            "https://filemoon.sx/e/cvnd9zqj2i9w" to "Filemoon",
            "https://bysekoze.com/e/y6kmotbkxiha/" to "Filemoon",
            "https://luluvdo.com/e/75n0j12zgsl3" to "Lulustream",
            "https://pelisplus.rpmstream.live/#abc123" to "RPMStream",
            "https://cubeembed.rpmvid.com/#fm8vz" to "RPMStream",
            "https://streamtape.com/e/abc123" to "Streamtape",
            "https://gettapeads.com/e/abc123" to "Streamtape",
            "https://swhoi.com/e/abc123" to "Streamwish",
            "https://streamwish.to/e/abc123" to "Streamwish",
            "https://vidhide.com/v/abc123" to "Vihide",
            "https://callistanise.com/v/jehmieb1epa5" to "Vihide",
            "https://niikaplayerr.com/embed/abc123" to "Vihide",
            "https://mixdrop.co/e/abcdef1234" to "Mixdrop",
            "https://mxdrop.to/e/abcdef1234" to "Mixdrop",
            "https://argtesa.top/?v=vu9jynYrzV" to "Abyss",
            "https://player.abyss.to/vu9jynYrzV" to "Abyss",
            "https://uqload.cx/embed-abc123.html" to "Uqload",
            "https://pixeldrain.com/u/abc123" to "Pixeldrain",
        ).forEach { (url, server) ->
            assertEquals(listOf(server), claimants(url), url)
        }
    }
}
