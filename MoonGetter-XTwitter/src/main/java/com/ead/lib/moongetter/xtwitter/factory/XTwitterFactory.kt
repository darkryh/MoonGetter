package com.ead.lib.moongetter.xtwitter.factory

import com.ead.lib.moongetter.models.Server
import com.ead.lib.moongetter.xtwitter.XTwitter

object XTwitterFactory : Server.Factory {
    override val belongedClass: Class<out Server> = XTwitter::class.java
    override val pattern: String = """https?:\/\/(?:www\.)?(?:twitter|x)\.com\/[A-Za-z0-9_]+\/status\/[0-9]+(?:\/video\/[0-9]+)?"""
}