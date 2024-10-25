package com.ead.lib.moongetter.vidguard.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.vidguard.Vidguard

object VidguardFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Vidguard::class.java
    override val pattern: String = "https?://(?:listeamed|vembed)\\.(?:com|net)/(?:e|d)/\\S+"
}