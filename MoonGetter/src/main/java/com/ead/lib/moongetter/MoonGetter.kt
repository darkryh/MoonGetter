package com.ead.lib.moongetter

import android.content.Context
import com.ead.lib.moongetter.models.Initializer
import com.ead.lib.moongetter.models.Request

open class MoonGetter {

    companion object {

        fun initialize(context: Context) : Request.Builder {
            return Initializer.Builder()
                .initialize(context)
        }

    }
}