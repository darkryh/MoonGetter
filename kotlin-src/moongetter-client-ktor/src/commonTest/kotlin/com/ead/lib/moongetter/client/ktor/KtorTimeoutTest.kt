package com.ead.lib.moongetter.client.ktor

import com.ead.lib.moongetter.client.cookie.managment.MoonCookie
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KtorTimeoutTest {
    @Test fun configuredTimeoutCancelsSlowRequestAndCanBeUpdated() = runBlocking {
        val client = KtorMoonClient(MockEngine, MoonCookie.Management.newEmptyFactory(),
            MoonTrust.Manager.newEmptyFactory()) {
            addHandler { delay(500); respond("OK") }
        }
        val request = Request<String>(method = HttpMethod.GET, url = "https://example.test/")
        client.initConfigurationData(Configuration.Data(timeout = 50, disableSSLValidation = false))
        assertFailsWith<HttpRequestTimeoutException> { client.request(request) }
        client.initConfigurationData(Configuration.Data(timeout = 2000, disableSSLValidation = false))
        assertEquals("OK", client.request(request).body.asString())
    }
}
