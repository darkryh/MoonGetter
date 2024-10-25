package com.ead.lib.moongetter.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.Exception

object Thread {

    private val handler = Handler(Looper.getMainLooper())

    fun delay(ms: Long,task: () -> Unit) {
        try {
            handler.postDelayed(task,ms)
        } catch (ex : Exception) {
            Log.e("error", "runInMs: $ex")
        }
    }

    fun onUi(action: () -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post(action)
        } else {
            action.invoke()
        }
    }
}