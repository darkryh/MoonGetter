package com.ead.project.moongetter.domain.custom_servers.test.las_estrellas.factory

import com.ead.lib.moongetter.models.Server
import com.ead.project.moongetter.domain.custom_servers.test.las_estrellas.LasEstrellas

object LasEstrellasFactory : Server.Factory {
    override val belongedClass: Class<out Server> = LasEstrellas::class.java
    override val pattern: String = """https://www\.lasestrellas\.tv/embed/[\w-]+/[\w-]+/[\w-]+(?:-[\w-]+)*"""
}