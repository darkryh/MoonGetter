package com.ead.lib.moongetter.fireload.factory

import com.ead.lib.moongetter.fireload.Fireload
import com.ead.lib.moongetter.models.Server

object FireloadFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Fireload::class.java
    override val pattern: String = "https?:\\/\\/(www\\.)?(fireload)\\.com\\/.+"
}