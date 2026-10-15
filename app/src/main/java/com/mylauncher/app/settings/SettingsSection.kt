package com.mylauncher.app.settings

/** The categories on the Settings dashboard. Each opens a page that holds the related settings. */
enum class SettingsSection(val title: String, val emoji: String, val description: String) {
    HOME("Home screen", "🏠", "Grid, dock, clock, search bar"),
    DRAWER("App drawer", "📱", "Layout, categories, search, recent apps"),
    WIDGETS("Widgets", "🧩", "Add and manage home screen widgets"),
    APPEARANCE("Appearance and themes", "🎨", "Themes, colors, wallpaper, animations"),
    ICONS("Icon packs and custom icons", "🎭", "Icon packs and your own icons"),
    BEHAVIOR("Performance and behavior", "⚡", "Gestures, default launcher, performance"),
    PRIVACY("Privacy and app control", "🔒", "Private apps, launcher lock, app lock"),
    MORE("Backup, device and about", "💾", "Backup, device dashboard, version"),
    ;

    companion object {
        /** Saved by name so a stale or unknown value simply shows the dashboard. */
        fun fromName(name: String?): SettingsSection? = entries.firstOrNull { it.name == name }
    }
}
