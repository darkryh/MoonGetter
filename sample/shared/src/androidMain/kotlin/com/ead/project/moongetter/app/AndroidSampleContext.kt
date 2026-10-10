package com.ead.project.moongetter.app

import android.content.Context

internal object AndroidSampleContext {
    lateinit var context: Context
}

/** Initialize application-scoped platform services before displaying the shared UI. */
fun initializeAndroidSample(context: Context) {
    AndroidSampleContext.context = context.applicationContext
    initializeKoin()
}
