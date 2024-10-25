package com.ead.lib.moongetter.yourupload.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.yourupload.YourUpload

object YourUploadFactory : Server.Factory {
    override val belongedClass: Class<out Server> = YourUpload::class.java
    override val pattern: String = """https?://.*yourupload\.com/embed/.*"""
}