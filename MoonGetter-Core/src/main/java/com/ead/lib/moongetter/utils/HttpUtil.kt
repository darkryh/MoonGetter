package com.ead.lib.moongetter.utils

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpUtil {
    const val BLANK_BROWSER = "about:blank"

    private var isDisableCertificationConnects = false

    @SuppressLint("CustomX509TrustManager","TrustAllX509TrustManager","TrustAllX509TrustManager")
    fun disableCertificationConnections(forceDisable: Boolean = false) {
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
            init(null, trustAllCerts, java.security.SecureRandom())
        }

        val sslSocketFactory = sslContext.socketFactory

        HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory)
    }
}