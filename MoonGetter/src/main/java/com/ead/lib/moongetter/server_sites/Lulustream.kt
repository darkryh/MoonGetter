package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.ServerJwPlayer

@Pending
@Unstable(reason = "Needs to validate requests headers")
class Lulustream(context: Context, url : String) : ServerJwPlayer(context,url) {
    override val isDeprecated: Boolean = true
    override suspend fun onExtract() = onDefaultJwPlayer()
}