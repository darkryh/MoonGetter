package com.ead.lib.moongetter.rpmstream
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.apple.Apple
internal actual fun rpmCryptoProvider(): CryptographyProvider = CryptographyProvider.Apple
