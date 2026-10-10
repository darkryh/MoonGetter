package com.ead.project.moongetter.app.network

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import io.ktor.http.Url
import kotlin.time.TimeSource

/** Sample diagnostics omit paths, query tokens, cookies and media URLs. */
class TimedMoonClient(private val delegate: MoonClient) : MoonClient {
    override fun initConfigurationData(configData: Configuration.Data) = delegate.initConfigurationData(configData)

    override suspend fun <T> request(request: Request<T>): Response {
        val host = Url(request.url).host
        val started = TimeSource.Monotonic.markNow()
        println("MoonGetter HTTP started: ${request.method} $host")
        try {
            return delegate.request(request).also {
                println("MoonGetter HTTP completed: $host status=${it.statusCode} finalHost=${it.url.host} duration=${started.elapsedNow()}")
            }
        } catch (failure: Exception) {
            println("MoonGetter HTTP failed: $host type=${failure::class.simpleName} duration=${started.elapsedNow()}")
            throw failure
        }
    }
}
