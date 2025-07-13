@file:Suppress("CustomX509TrustManager")

package com.ead.lib.moongetter.client.trust.manager.java.net

import com.ead.lib.moongetter.client.trust.manager.MoonClientTrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object JavaMoonClientTrustManager : MoonClientTrustManager {
    private var isDisableCertificationConnects = false

    override fun disableCertificationConnections(forceDisable: Boolean) {
        if (isDisableCertificationConnects && !forceDisable) return
        isDisableCertificationConnects = true
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                override fun checkClientTrusted(certs: Array<X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(certs: Array<X509Certificate>?, authType: String?) = Unit
            }
        )

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        val sslSocketFactory = sslContext.socketFactory

        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
    }
}