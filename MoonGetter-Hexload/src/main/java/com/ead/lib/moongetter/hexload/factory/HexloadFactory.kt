package com.ead.lib.moongetter.hexload.factory

import com.ead.lib.moongetter.hexload.Hexload
import com.ead.lib.moongetter.models.Server

object HexloadFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Hexload::class.java
    override val pattern: String = "https?://(?:hexload|hexupload)\\.(?:com|net)/embed-[^/]+\\.html"
}