package com.ead.lib.moongetter.client.models

class Configuration {
    data class Data(
        val timeout : Long = 8000,
        val isUsingRandomUserAgents : Boolean = true,
        val disableSSLValidation : Boolean = true
    )
}