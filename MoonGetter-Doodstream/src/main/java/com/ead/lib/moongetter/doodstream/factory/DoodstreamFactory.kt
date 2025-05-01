package com.ead.lib.moongetter.doodstream.factory

import com.ead.lib.moongetter.doodstream.Doodstream
import com.ead.lib.moongetter.models.Server

object DoodstreamFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Doodstream::class.java
    override val pattern: String = """https://do\d+[a-zA-Z]*\.[a-z]{2,}/e/([a-zA-Z0-9]+)"""
}