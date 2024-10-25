package com.ead.lib.moongetter.goodstream.factory

import com.ead.lib.moongetter.goodstream.GoodStream
import com.ead.lib.moongetter.models.Server

object GoodStreamFactory : Server.Factory {
    override val belongedClass: Class<out Server> = GoodStream::class.java
    override val pattern: String = """(https?://goodstream\.[^/]+/video/[^/]+)"""
}