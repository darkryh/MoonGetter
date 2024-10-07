package com.ead.lib.moongetter.core.system.extensions

import android.os.Build
import android.webkit.WebResourceError
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
fun WebResourceError.isNotThrowable() : Boolean {
    return errorCode != -1 || errorCode != -2
}