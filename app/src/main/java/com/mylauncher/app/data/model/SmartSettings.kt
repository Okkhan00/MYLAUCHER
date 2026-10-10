package com.mylauncher.app.data.model

import com.mylauncher.app.launcher.search.SearchEngine

/** Phase 3 "smart" options. Everything here is local; nothing is ever uploaded. */
data class SmartSettings(
    val smartSuggestions: Boolean = true,
    val webSearchEnabled: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customSearchUrl: String = "",
    val searchSettingsShortcuts: Boolean = true,
    val groupDrawerByCategory: Boolean = true,
    /** Show the "Recently used" row in the drawer. Needs launch memory (Privacy > Remember recently used apps). */
    val showRecentApps: Boolean = true,
)
