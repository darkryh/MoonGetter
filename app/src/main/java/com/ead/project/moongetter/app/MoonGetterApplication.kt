package com.ead.project.moongetter.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MoonGetterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MoonGetterApplication)
            modules(appModule)
        }
    }
}