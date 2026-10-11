package com.ead.lib.moongetter.resolution

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.HttpMethod
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.client.response.body.ResponseBody
import com.ead.lib.moongetter.client.response.url.Url

/**
 * Serves the page the fallback already fetched to the server's first GET of the same URL, then
 * delegates everything else. This avoids fetching the same page twice during a fallback resolution.
 */
internal class ReplayingMoonClient(
    private val delegate: MoonClient,
    private val page: PageSnapshot,
) : MoonClient {
    private var replayed = false

    override fun initConfigurationData(configData: Configuration.Data) = delegate.initConfigurationData(configData)

    override suspend fun <T> request(request: Request<T>): Response {
        val replayable = !replayed && request.method == HttpMethod.GET && request.queryParams.isEmpty() &&
            request.isResponseBodyNeeded && !page.isTruncated &&
            (request.url == page.finalUrl || request.url == page.requestedUrl)
        if (!replayable) return delegate.request(request)
        replayed = true
        return object : Response {
            override val statusCode: Int = page.statusCode
            override val headers: Map<String, String> = page.headers
            override val body: ResponseBody = object : ResponseBody {
                override fun asString(): String = page.body
            }
            override val url: Url = object : Url {
                override val toString: String = page.finalUrl
                override val host: String = EmbedUrl.parse(page.finalUrl)?.host.orEmpty()
            }
        }
    }
}
