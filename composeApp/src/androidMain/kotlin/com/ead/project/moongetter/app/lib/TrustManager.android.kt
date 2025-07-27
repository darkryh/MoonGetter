package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.trust.manager.MoonTrust
import com.ead.lib.moongetter.client.trust.manager.java.net.JavaMoonTrustManager

actual val trustManager: MoonTrust.Manager get() = JavaMoonTrustManager