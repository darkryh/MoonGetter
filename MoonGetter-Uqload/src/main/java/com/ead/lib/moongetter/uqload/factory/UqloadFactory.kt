@file:OptIn(ExperimentalServer::class)

package com.ead.lib.moongetter.uqload.factory

import com.ead.lib.moongetter.core.ExperimentalServer
import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.uqload.Uqload

object UqloadFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Uqload::class.java
    override val pattern: String = """https://uqload\.[a-z]+/embed-[\w\d]+\.html\d*"""
}