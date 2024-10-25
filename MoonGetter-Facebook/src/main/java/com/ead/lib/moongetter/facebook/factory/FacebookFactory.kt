package com.ead.lib.moongetter.facebook.factory

import com.ead.lib.moongetter.facebook.Facebook
import com.ead.lib.moongetter.models.Server

object FacebookFactory : Server.Factory {
    override val belongedClass: Class<out Server> = Facebook::class.java
    override val pattern: String = """https?://(www\.)?facebook\.com(?:/[^/]+)?/(videos|reel|watch)(?:/[^/?#]*)?/(\d+)/?"""
}