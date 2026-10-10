package com.ead.lib.moongetter.rpmstream
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK
internal actual fun rpmCryptoProvider(): CryptographyProvider = CryptographyProvider.JDK
