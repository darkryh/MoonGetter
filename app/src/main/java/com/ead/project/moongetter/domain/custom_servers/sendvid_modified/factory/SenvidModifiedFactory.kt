package com.ead.project.moongetter.domain.custom_servers.sendvid_modified.factory

import com.ead.lib.moongetter.models.Server
import com.ead.project.moongetter.domain.custom_servers.sendvid_modified.SenvidModified

object SenvidModifiedFactory : Server.Factory {
    override val belongedClass: Class<out Server> = SenvidModified::class.java
    override val pattern: String = """https://custom\.domain\.com/aqua/sv\?url=([^&]+)"""
}