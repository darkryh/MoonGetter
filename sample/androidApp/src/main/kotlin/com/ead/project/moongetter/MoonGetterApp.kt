package com.ead.project.moongetter

import android.app.Application
import com.ead.project.moongetter.app.initializeAndroidSample

class MoonGetterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeAndroidSample(this)
    }
}
