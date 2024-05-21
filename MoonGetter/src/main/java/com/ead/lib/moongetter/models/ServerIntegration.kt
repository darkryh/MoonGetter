package com.ead.lib.moongetter.models

data class ServerIntegration(
    val serverClass: Class<out Server>,
    val pattern: String
)