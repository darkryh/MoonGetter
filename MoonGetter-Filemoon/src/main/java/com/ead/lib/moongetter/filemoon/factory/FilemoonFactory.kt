package com.ead.lib.moongetter.filemoon.factory

import com.ead.lib.moongetter.filemoon.Filemoon
import com.ead.lib.moongetter.models.Server

object FilemoonFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Filemoon::class.java
    override val pattern: String = "https?://filemoon\\.sx/[^/]+"
}