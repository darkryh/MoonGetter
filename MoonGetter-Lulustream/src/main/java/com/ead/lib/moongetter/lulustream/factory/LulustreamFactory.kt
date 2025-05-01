@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.lulustream.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.lulustream.Lulustream
import com.ead.lib.moongetter.models.Server

object LulustreamFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Lulustream::class.java
    override val pattern: String = """https://lulu\w*\.\w+/e/[^\s"]+"""
}