package com.ead.lib.moongetter.server_sites

import android.content.Context
import com.ead.lib.moongetter.core.Pending
import com.ead.lib.moongetter.core.Unstable
import com.ead.lib.moongetter.models.Server

@Pending
@Unstable("There is some validations probably in the player side to accept the request")
class Mp4Upload(context: Context,url : String) : Server(context,url)