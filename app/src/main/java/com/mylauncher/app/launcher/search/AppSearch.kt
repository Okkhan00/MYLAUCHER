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
                compareBy<Pair<AppInfo, Int>> { it.second }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.label },
            )
            .map { it.first }
    }

    /**
     * Search data derived once per app list (lower-cased label, label words, lower-cased package), so
     * typing does not re-split and re-lower-case every label on every keystroke. Matching rules are
     * identical to [tier]; a unit test checks that both always agree.
     */
    class Index(val apps: List<AppInfo>) {
        private class Entry(val app: AppInfo, val label: String, val words: List<String>, val pkg: String)

        private val entries: List<Entry> = apps.map { app ->
            val label = app.label.lowercase()
            Entry(app, label, label.split(' ', '-', '_', '.'), app.packageName.lowercase())
        }

        /** Apps matching [query] with their tier, in list order (callers sort). */
        fun match(query: String): List<Pair<AppInfo, Int>> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return emptyList()
            val out = ArrayList<Pair<AppInfo, Int>>()
            for (e in entries) {
                val tier = when {
                    e.label == q -> TIER_EXACT
                    e.label.startsWith(q) -> TIER_PREFIX
                    e.words.any { it.startsWith(q) } -> TIER_WORD_PREFIX
                    e.label.contains(q) -> TIER_CONTAINS
                    e.pkg.contains(q) -> TIER_PACKAGE
                    else -> continue
                }
                out.add(e.app to tier)
            }
            return out
        }
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
