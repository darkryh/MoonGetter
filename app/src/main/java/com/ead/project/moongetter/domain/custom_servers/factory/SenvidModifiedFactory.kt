package com.ead.project.moongetter.domain.custom_servers.factory

import com.ead.lib.moongetter.models.Server
import com.ead.project.moongetter.domain.custom_servers.SenvidModified

object SenvidModifiedFactory : Server.Factory {
    override val belongedClass: Class<out Server> = SenvidModified::class.java
    override val pattern: String = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)"""
}