@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.mixdrop.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.mixdrop.Mixdrop
import com.ead.lib.moongetter.models.Server

object MixdropFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Mixdrop::class.java
    override val pattern: String = """https://m(i)?xdrop\.[a-z]{2,}/e/([a-zA-Z0-9]+)"""
}