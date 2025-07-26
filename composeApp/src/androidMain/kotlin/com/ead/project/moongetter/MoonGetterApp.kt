package com.ead.project.moongetter

import android.app.Application
import com.ead.project.moongetter.app.initializeKoin

class MoonGetterApp: Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        initializeKoin()
    }
    companion object {
        lateinit var instance: MoonGetterApp
        fun applicationContext(): MoonGetterApp {
            return instance
        }
    }
}