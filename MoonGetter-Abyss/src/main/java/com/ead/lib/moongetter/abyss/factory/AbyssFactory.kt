package com.ead.lib.moongetter.abyss.factory

import com.ead.lib.moongetter.abyss.Abyss
import com.ead.lib.moongetter.models.Server

object AbyssFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Abyss::class.java
    override val pattern: String = """https:\/\/abysscdn\.com\/\?v=[\w-]+"""
}