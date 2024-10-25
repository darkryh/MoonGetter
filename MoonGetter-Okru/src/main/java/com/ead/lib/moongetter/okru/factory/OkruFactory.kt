package com.ead.lib.moongetter.okru.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.okru.Okru

object OkruFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Okru::class.java
    override val pattern: String = "https?:\\/\\/(www\\.)?ok\\.ru\\/videoembed\\/.+"
}