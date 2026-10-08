package com.mylauncher.app.data

import android.content.Context
import com.mylauncher.app.data.preferences.LauncherPreferences
import com.mylauncher.app.data.preferences.launcherDataStore
import com.mylauncher.app.widgets.WidgetController

/** Manual dependency container (no DI framework needed for an app this size). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val preferences: LauncherPreferences by lazy { LauncherPreferences(appContext.launcherDataStore) }
    val appRepository: AppRepository by lazy { AppRepository(appContext) }

    /** One widget host for the whole process (created lazily; it holds only the application context). */
    val widgetController: WidgetController by lazy { WidgetController(appContext, preferences) }
}
