package com.ead.lib.moongetter.gofile.factory

import com.ead.lib.moongetter.gofile.Gofile
import com.ead.lib.moongetter.models.Server

object GofileFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Gofile::class.java
    override val pattern: String = """https://gofile\.io/d/(\w+)"""
}