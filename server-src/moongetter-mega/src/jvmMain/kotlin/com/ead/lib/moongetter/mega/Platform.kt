package com.ead.lib.moongetter.mega
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK
internal actual fun megaHttpClient(timeout:Long)=HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis=timeout;connectTimeoutMillis=timeout;socketTimeoutMillis=timeout };followRedirects=false }
internal actual fun megaCryptoProvider():CryptographyProvider=CryptographyProvider.JDK
