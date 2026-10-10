package com.mylauncher.app.data.model

/**
 * A launchable application. Identified by [packageName].
 *
 * [systemCategory] is the category the app declared to Android (ApplicationInfo.category), or -1
 * when unknown. It is read locally and only used as a hint for automatic categorization.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val systemCategory: Int = -1,
)
