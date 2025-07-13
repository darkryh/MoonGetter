package com.ead.lib.moongetter.client.trust.manager

interface MoonClientTrustManager {
    fun disableCertificationConnections(forceDisable: Boolean = false)
}