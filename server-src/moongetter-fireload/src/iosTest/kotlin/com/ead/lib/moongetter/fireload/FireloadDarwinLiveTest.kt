@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ead.lib.moongetter.fireload

import com.ead.lib.moongetter.client.cookie.managment.IosCookieManagement
import com.ead.lib.moongetter.client.ktor.KtorMoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import platform.posix.getenv
import kotlin.test.*

class FireloadDarwinLiveTest {
    @Test fun resolvesGuestTicketWithIosTransport() = runBlocking {
        val target = getenv("MOONGETTER_FIRELOAD_TEST_URL")?.toKString() ?: return@runBlocking
        println("Darwin test clock: ${platform.Foundation.NSDate()}")
        val client = KtorMoonClient(Darwin, IosCookieManagement(), MoonTrust.Manager.newEmptyFactory())
        val config = Configuration.Data(timeout = 60_000, disableSSLValidation = false)
        client.initConfigurationData(config)
        val request = Fireload(target, client, hashMapOf("User-Agent" to "Mozilla/5.0"), config).onExtract().single().request
        val id = FireloadPayload.fileId(target) ?: error("Invalid test input")
        assertTrue(FireloadPayload.validMedia(request.url, id))
        assertFalse(request.headers.orEmpty().keys.any { it.equals("Cookie", true) })
        println("Fireload live Darwin guest ticket and matching media server validated")
    }
}
