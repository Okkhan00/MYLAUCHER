package com.mylauncher.app.data.model

/** A launchable application. Identified by [packageName]. */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
)
