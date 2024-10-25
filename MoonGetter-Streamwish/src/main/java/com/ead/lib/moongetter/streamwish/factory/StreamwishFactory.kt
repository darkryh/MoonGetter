package com.ead.lib.moongetter.streamwish.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.streamwish.Streamwish

object StreamwishFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Streamwish::class.java
    override val pattern: String = "https?://(?:(?:[\\w-]*wish[\\w-]*)|(?:swhoi))\\.(?:com|to)/e/\\w+"
}