package com.mylauncher.app.launcher.search

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LaunchStat

/** A shortcut to an official Android settings screen. */
data class SettingShortcut(val id: String, val title: String, val intentAction: String, val keywords: List<String>)

/** Things the launcher itself can do. */
enum class SystemAction(val title: String, val keywords: List<String>) {
    LAUNCHER_SETTINGS("Launcher settings", listOf("launcher", "settings", "preferences", "options")),
    WALLPAPER("Change wallpaper", listOf("wallpaper", "background", "theme")),
    USAGE_INSIGHTS("Usage insights", listOf("usage", "insights", "stats", "statistics", "most used")),
    DEVICE_DASHBOARD("Device dashboard", listOf("device", "dashboard", "storage", "battery", "info")),
    BACKUP("Backup & restore", listOf("backup", "restore", "export", "import")),
    PRIVATE_APPS("Private apps", listOf("private", "hidden", "vault")),
    APP_CATEGORIES("App categories", listOf("categories", "category", "groups")),
}

sealed interface SearchResult {
    val key: String

    data class AppResult(val app: AppInfo, val inFolder: String? = null) : SearchResult {
        override val key: String get() = "app:" + app.packageName
    }

    data class SettingResult(val shortcut: SettingShortcut) : SearchResult {
        override val key: String get() = "setting:" + shortcut.id
    }

    data class ActionResult(val action: SystemAction) : SearchResult {
        override val key: String get() = "action:" + action.name
    }

    data class WebResult(val query: String) : SearchResult {
        override val key: String get() = "web"
    }
}

object SettingsShortcuts {
    val all: List<SettingShortcut> = listOf(
        SettingShortcut("wifi", "Wi-Fi settings", "android.settings.WIFI_SETTINGS", listOf("wifi", "wi-fi", "wireless", "network")),
        SettingShortcut("internet", "Internet settings", "android.settings.WIRELESS_SETTINGS", listOf("internet", "network", "mobile data", "cellular", "sim")),
        SettingShortcut("hotspot", "Hotspot & tethering", "android.settings.WIRELESS_SETTINGS", listOf("hotspot", "tethering", "tether", "wifi")),
        SettingShortcut("bluetooth", "Bluetooth settings", "android.settings.BLUETOOTH_SETTINGS", listOf("bluetooth", "pair", "devices")),
        SettingShortcut("airplane", "Airplane mode", "android.settings.AIRPLANE_MODE_SETTINGS", listOf("airplane", "flight mode", "aeroplane")),
        SettingShortcut("display", "Display settings", "android.settings.DISPLAY_SETTINGS", listOf("display", "brightness", "screen", "dark mode")),
        SettingShortcut("sound", "Sound settings", "android.settings.SOUND_SETTINGS", listOf("sound", "volume", "ringtone", "audio")),
        SettingShortcut("battery", "Battery settings", "android.settings.BATTERY_SAVER_SETTINGS", listOf("battery", "power", "saver")),
        SettingShortcut("storage", "Storage settings", "android.settings.INTERNAL_STORAGE_SETTINGS", listOf("storage", "space", "memory")),
        SettingShortcut("apps", "App settings", "android.settings.APPLICATION_SETTINGS", listOf("apps", "applications", "permissions")),
        SettingShortcut("location", "Location settings", "android.settings.LOCATION_SOURCE_SETTINGS", listOf("location", "gps")),
        SettingShortcut("data", "Data usage", "android.settings.DATA_USAGE_SETTINGS", listOf("data usage", "data", "mobile data")),
        SettingShortcut("nfc", "NFC settings", "android.settings.NFC_SETTINGS", listOf("nfc", "tap to pay")),
        SettingShortcut("date", "Date & time", "android.settings.DATE_SETTINGS", listOf("date", "time", "timezone")),
        SettingShortcut("language", "Language settings", "android.settings.LOCALE_SETTINGS", listOf("language", "locale", "region")),
        SettingShortcut("accessibility", "Accessibility", "android.settings.ACCESSIBILITY_SETTINGS", listOf("accessibility", "talkback", "font size")),
        SettingShortcut("security", "Security settings", "android.settings.SECURITY_SETTINGS", listOf("security", "lock screen", "fingerprint")),
        SettingShortcut("android", "Android settings", "android.settings.SETTINGS", listOf("settings", "android", "system")),
    )
}

/**
 * Smart Search 2.0. Pure and cheap: the app list is already cached in memory, so a keystroke only
 * costs a pass over that list plus two tiny static lists.
 *
 * App ranking: exact name, then prefix, then partial match; ties are broken by how often and then
 * how recently the app was launched.
 */
object SmartSearch {
    const val MIN_SHORTCUT_QUERY = 2
    const val MAX_SETTINGS = 4
    const val MAX_ACTIONS = 3

    fun search(
        query: String,
        apps: List<AppInfo>,
        folders: List<Folder> = emptyList(),
        stats: Map<String, LaunchStat> = emptyMap(),
        includeSettings: Boolean = true,
        includeActions: Boolean = true,
        includeWebFallback: Boolean = true,
    ): List<SearchResult> {
        val q = query.trim()
        if (q.isEmpty()) return apps.map { SearchResult.AppResult(it) }

        val byPackage = apps.associateBy { it.packageName }
        val tiered = apps.mapNotNull { app -> AppSearch.tier(app, q)?.let { app to it } }
        val appResults = tiered
            .sortedWith(
                compareBy<Pair<AppInfo, Int>> { it.second }
                    .thenByDescending { stats[it.first.packageName]?.count ?: 0 }
                    .thenByDescending { stats[it.first.packageName]?.lastLaunchedMs ?: 0L }
                    .thenBy { it.first.label.lowercase() },
            )
            .map { SearchResult.AppResult(it.first) as SearchResult }

        // Apps inside a folder whose name matches the query (apps already listed are not repeated).
        val listed = tiered.map { it.first.packageName }.toHashSet()
        val folderResults = folders
            .filter { it.name.contains(q, ignoreCase = true) }
            .flatMap { folder ->
                folder.appIds.mapNotNull { byPackage[it] }.map { SearchResult.AppResult(it, inFolder = folder.name) }
            }
            .filter { listed.add((it as SearchResult.AppResult).app.packageName) }

        val settingResults: List<SearchResult> =
            if (includeSettings && q.length >= MIN_SHORTCUT_QUERY) {
                SettingsShortcuts.all
                    .mapNotNull { s -> bestMatch(q, listOf(s.title) + s.keywords)?.let { s to it } }
                    .sortedBy { it.second }
                    .take(MAX_SETTINGS)
                    .map { SearchResult.SettingResult(it.first) }
            } else {
                emptyList()
            }

        val actionResults: List<SearchResult> =
            if (includeActions && q.length >= MIN_SHORTCUT_QUERY) {
                SystemAction.entries
                    .mapNotNull { a -> bestMatch(q, listOf(a.title) + a.keywords)?.let { a to it } }
                    .sortedBy { it.second }
                    .take(MAX_ACTIONS)
                    .map { SearchResult.ActionResult(it.first) }
            } else {
                emptyList()
            }

        val hasUsefulApp = tiered.any { it.second <= AppSearch.TIER_WORD_PREFIX }
        val web: List<SearchResult> =
            if (includeWebFallback && !hasUsefulApp) listOf(SearchResult.WebResult(q)) else emptyList()

        return appResults + folderResults + settingResults + actionResults + web
    }

    /** 0 = a phrase starts with the query, 1 = a word starts with it; null = no match. */
    private fun bestMatch(q: String, phrases: List<String>): Int? {
        var best: Int? = null
        for (p in phrases) {
            val rank = when {
                p.startsWith(q, ignoreCase = true) -> 0
                p.split(' ', '-', '&').any { it.startsWith(q, ignoreCase = true) } -> 1
                else -> null
            }
            if (rank != null && (best == null || rank < best)) best = rank
        }
        return best
    }
}
