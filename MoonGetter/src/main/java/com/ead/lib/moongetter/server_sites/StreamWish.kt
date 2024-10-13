package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.Properties
import com.ead.lib.moongetter.models.ServerJwPlayer

class StreamWish(context: Context, url : String) : ServerJwPlayer(context,url) {
    override val identifier: String = Properties.StreamWishIdentifier
    override val endingRegex: Regex = """https:\\/\\/mc\\.yandex\\.ru\\/clmap\\/.*""".toRegex()
    override suspend fun onExtract() = onDefaultJwPlayer()
}