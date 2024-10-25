package com.ead.lib.moongetter.pixeldrain.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.pixeldrain.Pixeldrain

object PixeldrainFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Pixeldrain::class.java
    override val pattern: String = "https?:\\/\\/pixeldrain\\.com\\/u\\/\\w+(?:\\?.*)?"
}