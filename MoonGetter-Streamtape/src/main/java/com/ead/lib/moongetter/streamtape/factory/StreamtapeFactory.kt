package com.ead.lib.moongetter.streamtape.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.streamtape.Streamtape

object StreamtapeFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Streamtape::class.java
    override val pattern: String = "https?://(?:streamtape|gettapeads)\\.com/e/\\w+"
}