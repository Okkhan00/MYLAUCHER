package com.mylauncher.app.launcher.apps

import com.mylauncher.app.data.model.AppInfo

object AppListFilter {
    /** Apps that should appear in the home screen, drawer and search. */
    fun visible(apps: List<AppInfo>, hidden: Set<String>): List<AppInfo> =
        if (hidden.isEmpty()) apps else apps.filter { it.packageName !in hidden }
}
