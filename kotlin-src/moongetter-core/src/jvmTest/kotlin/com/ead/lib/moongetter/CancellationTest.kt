package com.ead.lib.moongetter

import com.ead.lib.moongetter.client.MoonClient
import com.ead.lib.moongetter.client.models.Configuration
import com.ead.lib.moongetter.client.request.Request
import com.ead.lib.moongetter.client.response.Response
import com.ead.lib.moongetter.core.ExperimentalFeature
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.models.Video
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(ExperimentalFeature::class)
class CancellationTest {
    @Test
    fun cancellationEscapesNullableAndFallbackApis(): Unit = runBlocking {
        val previous = MoonGetter.instance
        val client = object : MoonClient {
            override fun initConfigurationData(configData: Configuration.Data) = Unit
            override suspend fun <T> request(request: Request<T>): Response = error("No network expected")
        }
        val provider = object : Server.Factory {
            override val serverName = "Cancelled"
            override val pattern = "https://cancelled.example/.*"
            override fun create(url: String, headers: HashMap<String, String>, configData: Configuration.Data, client: MoonClient): Server =
                object : Server(url, client, headers, configData) {
                    override suspend fun onExtract(): List<Video> = throw CancellationException("Cancelled extraction")
                }
        }
        try {
            MoonGetter.start(Factory.Builder().apply {
                this.client = client
                engine = Engine.Builder().onCore(arrayOf(provider)).build()
            })
            val url = "https://cancelled.example/video"
            assertFailsWith<CancellationException> { MoonGetter.getOrNull(url) }
            assertFailsWith<CancellationException> { MoonGetter.getUntilFindResource(listOf(url, url)) }
            assertFailsWith<CancellationException> { MoonGetter.getUntilFindResourceOrNull(listOf(url)) }
            assertFailsWith<CancellationException> { MoonGetter.get(listOf(url, url)) }
        } finally {
            MoonGetter.instance = previous
        }
    }
}
