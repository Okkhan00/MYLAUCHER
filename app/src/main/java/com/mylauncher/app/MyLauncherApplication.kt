package com.mylauncher.app

import android.app.Application
import com.mylauncher.app.data.AppContainer

class MyLauncherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Start loading the app list immediately so the drawer is ready when opened.
        container.appRepository
    }
}
