package com.ead.lib.moongetter.senvid.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.senvid.Senvid

object SenvidFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Senvid::class.java
    override val pattern: String = "https?:\\/\\/sendvid\\.com\\/\\w+"
}