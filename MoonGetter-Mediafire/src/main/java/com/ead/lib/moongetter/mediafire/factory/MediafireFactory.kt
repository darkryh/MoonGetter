package com.ead.lib.moongetter.mediafire.factory

import com.ead.lib.moongetter.mediafire.Mediafire
import com.ead.lib.moongetter.models.Server

object MediafireFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Mediafire::class.java
    override val pattern: String = """https?:\/\/(?:www\.)?mediafire\.com\/(?:file\/|\?.+)"""
}