package com.ead.lib.moongetter.abyss
import dev.whyoleg.cryptography.CryptographyProvider
import io.ktor.client.HttpClient
internal expect fun abyssHttpClient(timeout: Long): HttpClient
internal expect fun abyssCryptoProvider(): CryptographyProvider
