package com.ead.lib.moongetter.voe.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.voe.Voe

object VoeFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Voe::class.java
    override val pattern: String = "https?://(?:voe|markstyleall|shannonpersonalcost|cindyeyefinal)\\.(?:com|sx)/(?:e|d)/\\w+"
}