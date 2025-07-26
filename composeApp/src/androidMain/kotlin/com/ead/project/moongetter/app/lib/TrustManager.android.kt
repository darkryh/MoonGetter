package com.ead.project.moongetter.app.lib

import com.ead.lib.moongetter.client.trust.manager.MoonClientTrustManager
import com.ead.lib.moongetter.client.trust.manager.java.net.JavaMoonClientTrustManager

actual val trustManager: MoonClientTrustManager get() = JavaMoonClientTrustManager