package com.ead.lib.moongetter.client.models

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

class Configuration {
    @OptIn(ExperimentalObjCName::class)
    @ObjCName("ClientConfigurationData", exact = true)
    data class Data(
        val timeout : Long = 8000,
        val isUsingRandomUserAgents : Boolean = true,
        val disableSSLValidation : Boolean = true
    )
}