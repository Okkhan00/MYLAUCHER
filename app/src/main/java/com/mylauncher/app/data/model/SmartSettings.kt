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
)
