package com.mylauncher.app.launcher.apps

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LaunchStat

/** Launcher-level history only: apps opened from this launcher. No special permissions involved. */
object LaunchHistory {
    fun recent(stats: Map<String, LaunchStat>, apps: List<AppInfo>, limit: Int = 5): List<AppInfo> {
        val byPackage = apps.associateBy { it.packageName }
        return stats.entries
            .sortedByDescending { it.value.lastLaunchedMs }
            .mapNotNull { byPackage[it.key] }
            .take(limit)
    }

    fun mostUsed(stats: Map<String, LaunchStat>, apps: List<AppInfo>, limit: Int = 5, minCount: Int = 3): List<AppInfo> {
        val byPackage = apps.associateBy { it.packageName }
        return stats.entries
            .filter { it.value.count >= minCount }
            .sortedWith(
                compareByDescending<Map.Entry<String, LaunchStat>> { it.value.count }
                    .thenByDescending { it.value.lastLaunchedMs },
            )
            .mapNotNull { byPackage[it.key] }
            .take(limit)
    }
}
