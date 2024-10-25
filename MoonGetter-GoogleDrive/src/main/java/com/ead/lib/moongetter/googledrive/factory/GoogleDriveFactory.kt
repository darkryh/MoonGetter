package com.ead.lib.moongetter.googledrive.factory

import com.ead.lib.moongetter.googledrive.GoogleDrive
import com.ead.lib.moongetter.models.Server

object GoogleDriveFactory : Server.Factory {
    override val belongedClass: Class<out Server> = GoogleDrive::class.java
    override val pattern: String = "https?:\\/\\/(www\\.)?drive\\.google\\.com\\/(?:file\\/d\\/|open\\?id=|uc\\?id=)([\\w-]+)"
}