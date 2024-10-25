package com.ead.lib.moongetter.mp4upload.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.mp4upload.Mp4Upload

object Mp4UploadFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Mp4Upload::class.java
    override val pattern: String = """https://www\.mp4upload\.[a-z]+/embed-[\w\d]+\.html"""
}