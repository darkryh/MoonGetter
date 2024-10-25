package com.ead.lib.moongetter.onecloudfile.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.onecloudfile.OneCloudFile

object OneCloudFileFactory : Server.Factory {
    override val belongedClass: Class<out Server> = OneCloudFile::class.java
    override val pattern: String = "https?:\\/\\/1cloudfile\\.com\\/\\w+"
}