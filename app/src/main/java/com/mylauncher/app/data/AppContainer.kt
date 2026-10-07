package com.mylauncher.app.data

import android.content.Context
import com.mylauncher.app.data.preferences.LauncherPreferences
import com.mylauncher.app.data.preferences.launcherDataStore

/** Manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val preferences: LauncherPreferences by lazy { LauncherPreferences(appContext.launcherDataStore) }
    val appRepository: AppRepository by lazy { AppRepository(appContext) }
}
