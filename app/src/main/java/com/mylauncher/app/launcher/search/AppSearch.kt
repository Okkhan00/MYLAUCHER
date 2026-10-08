package com.mylauncher.app.launcher.search

import com.mylauncher.app.data.model.AppInfo

/** Pure, instant, case-insensitive app search. */
object AppSearch {
    const val TIER_EXACT = 0
    const val TIER_PREFIX = 1
    const val TIER_WORD_PREFIX = 2
    const val TIER_CONTAINS = 3
    const val TIER_PACKAGE = 4

    /** Returns apps matching [query], best matches first. Blank query returns [apps] unchanged. */
    fun filter(apps: List<AppInfo>, query: String): List<AppInfo> {
        val q = query.trim()
        if (q.isEmpty()) return apps
        return apps
            .mapNotNull { app -> tier(app, q)?.let { app to it } }
            .sortedWith(
                compareBy<Pair<AppInfo, Int>>({ it.second }, { it.first.label.lowercase() }),
            )
            .map { it.first }
    }

    /** Lower is better; null means no match. Exact label match beats prefix, prefix beats partial. */
    fun tier(app: AppInfo, q: String): Int? = when {
        app.label.equals(q, ignoreCase = true) -> TIER_EXACT
        app.label.startsWith(q, ignoreCase = true) -> TIER_PREFIX
        app.label.split(' ', '-', '_', '.').any { it.startsWith(q, ignoreCase = true) } -> TIER_WORD_PREFIX
        app.label.contains(q, ignoreCase = true) -> TIER_CONTAINS
        app.packageName.contains(q, ignoreCase = true) -> TIER_PACKAGE
        else -> null
    }
}
