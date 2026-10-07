package com.mylauncher.app.launcher.search

import com.mylauncher.app.data.model.AppInfo

/** Pure, instant, case-insensitive app search. */
object AppSearch {

    /** Returns apps matching [query], best matches first. Blank query returns [apps] unchanged. */
    fun filter(apps: List<AppInfo>, query: String): List<AppInfo> {
        val q = query.trim()
        if (q.isEmpty()) return apps
        return apps
            .mapNotNull { app -> rank(app, q)?.let { app to it } }
            .sortedWith(
                compareBy<Pair<AppInfo, Int>>({ it.second }, { it.first.label.lowercase() }),
            )
            .map { it.first }
    }

    /** Lower is better; null means no match. */
    private fun rank(app: AppInfo, q: String): Int? = when {
        app.label.startsWith(q, ignoreCase = true) -> 0
        app.label.split(' ', '-', '_', '.').any { it.startsWith(q, ignoreCase = true) } -> 1
        app.label.contains(q, ignoreCase = true) -> 2
        app.packageName.contains(q, ignoreCase = true) -> 3
        else -> null
    }
}
