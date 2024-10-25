package com.ead.lib.moongetter.vihide.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.vihide.Vihide

object VihideFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Vihide::class.java
    override val pattern: String = """https?://(?:www\.)?(vidhide[a-zA-Z]*\.com)/[a-zA-Z0-9]+/[a-zA-Z0-9]+"""
}