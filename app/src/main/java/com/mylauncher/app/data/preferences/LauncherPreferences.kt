package com.mylauncher.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryCodec
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LaunchEvent
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.timer.AppTimer
import com.mylauncher.app.timer.PinGuard
import com.mylauncher.app.timer.TimerCodec
import com.mylauncher.app.timer.TimerRules
import com.mylauncher.app.timer.TimerState
import com.mylauncher.app.ui.theme.CustomThemeCodec
import com.mylauncher.app.data.model.WallpaperSettings
import com.mylauncher.app.backup.BackupSchema
import com.mylauncher.app.backup.BackupType
import com.mylauncher.app.widgets.HostedWidget
import com.mylauncher.app.widgets.WidgetCodec
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.data.model.folderToken
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal object PrefKeys {
    val THEME = stringPreferencesKey("theme")
    val ICON_SIZE = stringPreferencesKey("icon_size")
    val COLUMNS = intPreferencesKey("columns")
    val SHOW_LABELS = booleanPreferencesKey("show_labels")
    val ANIMATIONS = booleanPreferencesKey("animations")
    val CLOCK_STYLE = stringPreferencesKey("clock_style")
    val SHOW_DATE = booleanPreferencesKey("show_date")
    val SHOW_SEARCH_BAR = booleanPreferencesKey("show_search_bar")
    val DRAWER_LAYOUT = stringPreferencesKey("drawer_layout")
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
    val SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
    val DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
    val SHOW_BATTERY = booleanPreferencesKey("show_battery")
    val TRACK_LAUNCHES = booleanPreferencesKey("track_launches")
    val FAVORITES = stringPreferencesKey("favorites") // ordered home list: packages and "folder:<id>" tokens
    val HIDDEN = stringPreferencesKey("hidden")
    val FOLDERS = stringPreferencesKey("folders")
    val LAUNCH_STATS = stringPreferencesKey("launch_stats")
    val PIN_HASH = stringPreferencesKey("pin_hash")
    val PIN_SALT = stringPreferencesKey("pin_salt")
    val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
    val PROTECT_SETTINGS = booleanPreferencesKey("protect_settings")
    val PROTECT_HIDDEN = booleanPreferencesKey("protect_hidden")

    // ---- Phase 3 (new keys only; every Phase 1/2 key above keeps its name and meaning) ----
    val SMART_SUGGESTIONS = booleanPreferencesKey("smart_suggestions")
    val WEB_SEARCH = booleanPreferencesKey("search_web_enabled")
    val SEARCH_ENGINE = stringPreferencesKey("search_engine")
    val SEARCH_CUSTOM_URL = stringPreferencesKey("search_custom_url")
    val SEARCH_SHORTCUTS = booleanPreferencesKey("search_settings_shortcuts")
    val DRAWER_BY_CATEGORY = booleanPreferencesKey("drawer_group_by_category")
    val SHOW_RECENT_APPS = booleanPreferencesKey("show_recent_apps")
    val CATEGORY_OVERRIDES = stringPreferencesKey("category_overrides")
    val CATEGORY_CONFIG = stringPreferencesKey("category_config")
    val LAUNCH_LOG = stringPreferencesKey("launch_log")

    // Themes and wallpaper
    val THEME_DYNAMIC = booleanPreferencesKey("theme_dynamic_color")
    val THEME_ACCENT = intPreferencesKey("theme_accent_color")
    val THEME_SURFACE = stringPreferencesKey("theme_surface_style")
    val THEME_TEXT_SCALE = stringPreferencesKey("theme_text_scale")
    val THEME_SEARCH_BAR = stringPreferencesKey("theme_search_bar_style")
    val THEME_RADIUS = intPreferencesKey("theme_corner_radius")
    val THEME_DRAWER_OPACITY = intPreferencesKey("theme_drawer_opacity")
    val THEME_CUSTOM = stringPreferencesKey("theme_custom")
    val TIMERS_ENABLED = booleanPreferencesKey("timers_enabled")
    val TIMERS = stringPreferencesKey("app_timers")
    val TIMER_PIN_HASH = stringPreferencesKey("timer_pin_hash")
    val TIMER_PIN_SALT = stringPreferencesKey("timer_pin_salt")
    val TIMER_BIOMETRIC = booleanPreferencesKey("timer_biometric")
    val TIMER_PIN_GUARD = stringPreferencesKey("timer_pin_guard")
    val WALLPAPER_OVERLAY = stringPreferencesKey("wallpaper_overlay")
    val WALLPAPER_OVERLAY_PERCENT = intPreferencesKey("wallpaper_overlay_percent")
    val WALLPAPER_BLUR = stringPreferencesKey("wallpaper_blur")
    val WALLPAPER_DRAWER_OVERLAY = booleanPreferencesKey("wallpaper_drawer_overlay")

    // App lock (launcher level): packages that need the launcher PIN/biometrics before launching
    val LOCKED_APPS = stringPreferencesKey("locked_apps")

    val PERFORMANCE_MODE = stringPreferencesKey("performance_mode")

    // Widgets: placed widgets, and an id that was allocated but not yet confirmed (cleaned up on next start)
    val WIDGETS = stringPreferencesKey("widgets")
    val WIDGET_PENDING = intPreferencesKey("widget_pending_id")
}

private inline fun <reified E : Enum<E>> parseEnum(raw: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == raw } ?: default

/**
 * All persisted launcher state. Missing or corrupted values fall back to defaults.
 * Takes a [DataStore] so it can be tested without Android.
 */
class LauncherPreferences(private val store: DataStore<Preferences>) {

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<LauncherSettings> = data
        .map { p ->
            val d = LauncherSettings()
            LauncherSettings(
                theme = parseEnum(p[PrefKeys.THEME], d.theme),
                iconSize = parseEnum(p[PrefKeys.ICON_SIZE], d.iconSize),
                columns = (p[PrefKeys.COLUMNS] ?: d.columns)
                    .coerceIn(LauncherSettings.MIN_COLUMNS, LauncherSettings.MAX_COLUMNS),
                showLabels = p[PrefKeys.SHOW_LABELS] ?: d.showLabels,
                animations = p[PrefKeys.ANIMATIONS] ?: d.animations,
                clockStyle = parseEnum(p[PrefKeys.CLOCK_STYLE], d.clockStyle),
                showDate = p[PrefKeys.SHOW_DATE] ?: d.showDate,
                showSearchBar = p[PrefKeys.SHOW_SEARCH_BAR] ?: d.showSearchBar,
                drawerLayout = parseEnum(p[PrefKeys.DRAWER_LAYOUT], d.drawerLayout),
                swipeUp = parseEnum(p[PrefKeys.SWIPE_UP], d.swipeUp),
                swipeDown = parseEnum(p[PrefKeys.SWIPE_DOWN], d.swipeDown),
                doubleTap = parseEnum(p[PrefKeys.DOUBLE_TAP], d.doubleTap),
                showBattery = p[PrefKeys.SHOW_BATTERY] ?: d.showBattery,
                trackLaunches = p[PrefKeys.TRACK_LAUNCHES] ?: d.trackLaunches,
                onboardingDone = p[PrefKeys.ONBOARDING_DONE] ?: d.onboardingDone,
                performance = parseEnum(p[PrefKeys.PERFORMANCE_MODE], d.performance),
            )
        }
        .catch { emit(LauncherSettings()) }

    /** Ordered home list: package names and "folder:<id>" tokens. */
    val favorites: Flow<List<String>> = data.map { ListCodec.decode(it[PrefKeys.FAVORITES]) }

    val hidden: Flow<Set<String>> = data.map { ListCodec.decode(it[PrefKeys.HIDDEN]).toSet() }

    val folders: Flow<List<Folder>> = data.map { FolderCodec.decode(it[PrefKeys.FOLDERS]) }

    val launchStats: Flow<Map<String, LaunchStat>> = data.map { LaunchStatsCodec.decode(it[PrefKeys.LAUNCH_STATS]) }

    val smartSettings: Flow<SmartSettings> = data.map { p ->
        val d = SmartSettings()
        SmartSettings(
            smartSuggestions = p[PrefKeys.SMART_SUGGESTIONS] ?: d.smartSuggestions,
            webSearchEnabled = p[PrefKeys.WEB_SEARCH] ?: d.webSearchEnabled,
            searchEngine = parseEnum(p[PrefKeys.SEARCH_ENGINE], d.searchEngine),
            customSearchUrl = p[PrefKeys.SEARCH_CUSTOM_URL] ?: d.customSearchUrl,
            searchSettingsShortcuts = p[PrefKeys.SEARCH_SHORTCUTS] ?: d.searchSettingsShortcuts,
            groupDrawerByCategory = p[PrefKeys.DRAWER_BY_CATEGORY] ?: d.groupDrawerByCategory,
            showRecentApps = p[PrefKeys.SHOW_RECENT_APPS] ?: d.showRecentApps,
        )
    }

    val categoryOverrides: Flow<Map<String, AppCategory>> = data.map { CategoryCodec.decodeOverrides(it[PrefKeys.CATEGORY_OVERRIDES]) }

    val categoryConfig: Flow<CategoryConfig> = data.map { CategoryCodec.decodeConfig(it[PrefKeys.CATEGORY_CONFIG]) }

    val launchLog: Flow<List<LaunchEvent>> = data.map { LaunchLogCodec.decode(it[PrefKeys.LAUNCH_LOG]) }

    val themeSettings: Flow<ThemeSettings> = data.map { p ->
        val d = ThemeSettings()
        ThemeSettings(
            dynamicColor = p[PrefKeys.THEME_DYNAMIC] ?: d.dynamicColor,
            accentColor = p[PrefKeys.THEME_ACCENT] ?: d.accentColor,
            surfaceStyle = parseEnum(p[PrefKeys.THEME_SURFACE], d.surfaceStyle),
            textScale = parseEnum(p[PrefKeys.THEME_TEXT_SCALE], d.textScale),
            searchBarStyle = parseEnum(p[PrefKeys.THEME_SEARCH_BAR], d.searchBarStyle),
            cornerRadius = (p[PrefKeys.THEME_RADIUS] ?: d.cornerRadius)
                .coerceIn(ThemeSettings.MIN_RADIUS, ThemeSettings.MAX_RADIUS),
            drawerOpacity = (p[PrefKeys.THEME_DRAWER_OPACITY] ?: d.drawerOpacity)
                .coerceIn(ThemeSettings.MIN_DRAWER_OPACITY, ThemeSettings.MAX_DRAWER_OPACITY),
            custom = CustomThemeCodec.decode(p[PrefKeys.THEME_CUSTOM]),
        )
    }

    val wallpaperSettings: Flow<WallpaperSettings> = data.map { p ->
        val d = WallpaperSettings()
        WallpaperSettings(
            overlay = parseEnum(p[PrefKeys.WALLPAPER_OVERLAY], d.overlay),
            overlayPercent = (p[PrefKeys.WALLPAPER_OVERLAY_PERCENT] ?: d.overlayPercent)
                .coerceIn(WallpaperSettings.MIN_OVERLAY_PERCENT, WallpaperSettings.MAX_OVERLAY_PERCENT),
            blur = parseEnum(p[PrefKeys.WALLPAPER_BLUR], d.blur),
            drawerOverlay = p[PrefKeys.WALLPAPER_DRAWER_OVERLAY] ?: d.drawerOverlay,
        )
    }

    val widgets: Flow<List<HostedWidget>> = data.map { WidgetCodec.decode(it[PrefKeys.WIDGETS]) }

    val lockedApps: Flow<Set<String>> = data.map { ListCodec.decode(it[PrefKeys.LOCKED_APPS]).toSet() }

    val timerState: Flow<TimerState> = data.map { p ->
        TimerState(
            enabled = p[PrefKeys.TIMERS_ENABLED] ?: false,
            timers = TimerCodec.decode(p[PrefKeys.TIMERS]),
            pinHash = p[PrefKeys.TIMER_PIN_HASH],
            pinSalt = p[PrefKeys.TIMER_PIN_SALT],
            biometric = p[PrefKeys.TIMER_BIOMETRIC] ?: false,
        )
    }

    val security: Flow<SecuritySettings> = data.map { p ->
        SecuritySettings(
            pinHash = p[PrefKeys.PIN_HASH],
            pinSalt = p[PrefKeys.PIN_SALT],
            biometricEnabled = p[PrefKeys.BIOMETRIC] ?: false,
            protectSettings = p[PrefKeys.PROTECT_SETTINGS] ?: true,
            protectHidden = p[PrefKeys.PROTECT_HIDDEN] ?: true,
        )
    }

    suspend fun saveSettings(s: LauncherSettings) {
        store.edit { p ->
            p[PrefKeys.THEME] = s.theme.name
            p[PrefKeys.ICON_SIZE] = s.iconSize.name
            p[PrefKeys.COLUMNS] = s.columns.coerceIn(LauncherSettings.MIN_COLUMNS, LauncherSettings.MAX_COLUMNS)
            p[PrefKeys.SHOW_LABELS] = s.showLabels
            p[PrefKeys.ANIMATIONS] = s.animations
            p[PrefKeys.CLOCK_STYLE] = s.clockStyle.name
            p[PrefKeys.SHOW_DATE] = s.showDate
            p[PrefKeys.SHOW_SEARCH_BAR] = s.showSearchBar
            p[PrefKeys.DRAWER_LAYOUT] = s.drawerLayout.name
            p[PrefKeys.SWIPE_UP] = s.swipeUp.name
            p[PrefKeys.SWIPE_DOWN] = s.swipeDown.name
            p[PrefKeys.DOUBLE_TAP] = s.doubleTap.name
            p[PrefKeys.SHOW_BATTERY] = s.showBattery
            p[PrefKeys.TRACK_LAUNCHES] = s.trackLaunches
            p[PrefKeys.ONBOARDING_DONE] = s.onboardingDone
            p[PrefKeys.PERFORMANCE_MODE] = s.performance.name
        }
    }

    // ---- Home list (apps and folders) -------------------------------------------------------

    /** Puts an app on the home screen. If it was inside a folder it leaves that folder. */
    suspend fun addFavorite(pkg: String) {
        store.edit { p ->
            p[PrefKeys.FAVORITES] = ListCodec.encode(ListOps.add(ListCodec.decode(p[PrefKeys.FAVORITES]), pkg))
            p[PrefKeys.FOLDERS] = FolderCodec.encode(FolderOps.removeAppEverywhere(FolderCodec.decode(p[PrefKeys.FOLDERS]), pkg))
        }
    }

    /** [token] is a package name or a "folder:<id>" token. */
    suspend fun removeFavorite(token: String) = editList(PrefKeys.FAVORITES) { ListOps.remove(it, token) }

    suspend fun moveFavorite(token: String, delta: Int) =
        editList(PrefKeys.FAVORITES) { ListOps.move(it, token, delta) }

    /** Saves the order the user dragged the visible home items into. */
    suspend fun setFavoritesOrder(visibleOrder: List<String>) =
        editList(PrefKeys.FAVORITES) { ListOps.reorder(it, visibleOrder) }

    /** Hides an app from the launcher (not from Android) and drops it from the home screen. */
    suspend fun hide(pkg: String) {
        store.edit { p ->
            p[PrefKeys.HIDDEN] = ListCodec.encode(ListOps.add(ListCodec.decode(p[PrefKeys.HIDDEN]), pkg))
            p[PrefKeys.FAVORITES] = ListCodec.encode(ListOps.remove(ListCodec.decode(p[PrefKeys.FAVORITES]), pkg))
        }
    }

    suspend fun unhide(pkg: String) = editList(PrefKeys.HIDDEN) { ListOps.remove(it, pkg) }

    // ---- Folders ---------------------------------------------------------------------------

    suspend fun createFolder(name: String, id: String = UUID.randomUUID().toString()): String {
        store.edit { p ->
            p[PrefKeys.FOLDERS] = FolderCodec.encode(FolderOps.create(FolderCodec.decode(p[PrefKeys.FOLDERS]), id, name))
            p[PrefKeys.FAVORITES] = ListCodec.encode(ListOps.add(ListCodec.decode(p[PrefKeys.FAVORITES]), folderToken(id)))
        }
        return id
    }

    suspend fun renameFolder(id: String, name: String) = editFolders { FolderOps.rename(it, id, name) }

    suspend fun deleteFolder(id: String) {
        store.edit { p ->
            p[PrefKeys.FOLDERS] = FolderCodec.encode(FolderOps.delete(FolderCodec.decode(p[PrefKeys.FOLDERS]), id))
            p[PrefKeys.FAVORITES] = ListCodec.encode(ListOps.remove(ListCodec.decode(p[PrefKeys.FAVORITES]), folderToken(id)))
        }
    }

    /** Moves an app into a folder; it leaves the home list and any other folder. */
    suspend fun addAppToFolder(id: String, pkg: String) {
        store.edit { p ->
            val before = FolderCodec.decode(p[PrefKeys.FOLDERS])
            if (before.none { it.id == id }) return@edit
            p[PrefKeys.FOLDERS] = FolderCodec.encode(FolderOps.addApp(before, id, pkg))
            p[PrefKeys.FAVORITES] = ListCodec.encode(ListOps.remove(ListCodec.decode(p[PrefKeys.FAVORITES]), pkg))
        }
    }

    suspend fun removeAppFromFolder(id: String, pkg: String) = editFolders { FolderOps.removeApp(it, id, pkg) }

    suspend fun moveAppInFolder(id: String, pkg: String, delta: Int) =
        editFolders { FolderOps.moveApp(it, id, pkg, delta) }

    suspend fun setFolderOrder(id: String, visibleOrder: List<String>) =
        editFolders { FolderOps.reorderApps(it, id, visibleOrder) }

    // ---- Launch history --------------------------------------------------------------------

    suspend fun recordLaunch(pkg: String, nowMs: Long) {
        store.edit { p ->
            val updated = LaunchStatsCodec.record(LaunchStatsCodec.decode(p[PrefKeys.LAUNCH_STATS]), pkg, nowMs)
            p[PrefKeys.LAUNCH_STATS] = LaunchStatsCodec.encode(updated)
            p[PrefKeys.LAUNCH_LOG] = LaunchLogCodec.encode(LaunchLogCodec.append(LaunchLogCodec.decode(p[PrefKeys.LAUNCH_LOG]), pkg, nowMs))
        }
    }

    suspend fun clearLaunchHistory() {
        store.edit {
            it.remove(PrefKeys.LAUNCH_STATS)
            it.remove(PrefKeys.LAUNCH_LOG)
        }
    }

    // ---- Themes, wallpaper, app lock --------------------------------------------------------

    suspend fun saveThemeSettings(t: ThemeSettings) {
        store.edit { p ->
            p[PrefKeys.THEME_DYNAMIC] = t.dynamicColor
            p[PrefKeys.THEME_ACCENT] = t.accentColor
            p[PrefKeys.THEME_SURFACE] = t.surfaceStyle.name
            p[PrefKeys.THEME_TEXT_SCALE] = t.textScale.name
            p[PrefKeys.THEME_SEARCH_BAR] = t.searchBarStyle.name
            p[PrefKeys.THEME_RADIUS] = t.cornerRadius.coerceIn(ThemeSettings.MIN_RADIUS, ThemeSettings.MAX_RADIUS)
            p[PrefKeys.THEME_DRAWER_OPACITY] =
                t.drawerOpacity.coerceIn(ThemeSettings.MIN_DRAWER_OPACITY, ThemeSettings.MAX_DRAWER_OPACITY)
            p[PrefKeys.THEME_CUSTOM] = CustomThemeCodec.encode(t.custom)
        }
    }

    /**
     * Applies a theme in one write: the mode lives with the launcher settings and the colors with the theme
     * settings, and doing both inside a single DataStore edit means a restart can never see half of it.
     */
    suspend fun applyThemeChoice(settings: LauncherSettings, theme: ThemeSettings) {
        store.edit { p ->
            p[PrefKeys.THEME] = settings.theme.name
            p[PrefKeys.THEME_DYNAMIC] = theme.dynamicColor
            p[PrefKeys.THEME_ACCENT] = theme.accentColor
            p[PrefKeys.THEME_SURFACE] = theme.surfaceStyle.name
            p[PrefKeys.THEME_CUSTOM] = CustomThemeCodec.encode(theme.custom)
        }
    }

    suspend fun saveWallpaperSettings(w: WallpaperSettings) {
        store.edit { p ->
            p[PrefKeys.WALLPAPER_OVERLAY] = w.overlay.name
            p[PrefKeys.WALLPAPER_OVERLAY_PERCENT] =
                w.overlayPercent.coerceIn(WallpaperSettings.MIN_OVERLAY_PERCENT, WallpaperSettings.MAX_OVERLAY_PERCENT)
            p[PrefKeys.WALLPAPER_BLUR] = w.blur.name
            p[PrefKeys.WALLPAPER_DRAWER_OVERLAY] = w.drawerOverlay
        }
    }

    suspend fun setAppLocked(pkg: String, locked: Boolean) =
        editList(PrefKeys.LOCKED_APPS) { if (locked) ListOps.add(it, pkg) else ListOps.remove(it, pkg) }

    // ---- Widgets ----------------------------------------------------------------------------

    suspend fun addWidget(widget: HostedWidget) = editWidgets { WidgetCodec.add(it, widget) }

    suspend fun removeWidget(id: Int) = editWidgets { WidgetCodec.remove(it, id) }

    suspend fun moveWidget(id: Int, delta: Int) = editWidgets { WidgetCodec.move(it, id, delta) }

    suspend fun resizeWidget(id: Int, delta: Int) = editWidgets { WidgetCodec.resize(it, id, delta) }

    suspend fun pendingWidgetId(): Int? = data.first()[PrefKeys.WIDGET_PENDING]

    suspend fun setPendingWidget(id: Int?) {
        store.edit { p -> if (id == null) p.remove(PrefKeys.WIDGET_PENDING) else p[PrefKeys.WIDGET_PENDING] = id }
    }

    private suspend fun editWidgets(transform: (List<HostedWidget>) -> List<HostedWidget>) {
        store.edit { p -> p[PrefKeys.WIDGETS] = WidgetCodec.encode(transform(WidgetCodec.decode(p[PrefKeys.WIDGETS]))) }
    }

    // ---- Backup and restore -----------------------------------------------------------------

    /** Current values of every key a backup may contain (see [BackupSchema]). Secrets are not in that list. */
    suspend fun exportBackupValues(): Map<String, Any> {
        val out = LinkedHashMap<String, Any>()
        for ((key, value) in data.first().asMap()) {
            val type = BackupSchema.keys[key.name] ?: continue
            val ok = when (type) {
                BackupType.STRING -> value is String
                BackupType.BOOLEAN -> value is Boolean
                BackupType.INT -> value is Int
            }
            if (ok) out[key.name] = value
        }
        return out
    }

    /**
     * Replaces every backup-covered setting with the validated [values] in one atomic edit. Keys the
     * backup does not mention are reset to their defaults. PIN, launch history and widgets are untouched.
     */
    suspend fun importBackupValues(values: Map<String, Any>) {
        store.edit { p ->
            for ((name, type) in BackupSchema.keys) {
                when (type) {
                    BackupType.STRING -> p.remove(stringPreferencesKey(name))
                    BackupType.BOOLEAN -> p.remove(booleanPreferencesKey(name))
                    BackupType.INT -> p.remove(intPreferencesKey(name))
                }
            }
            for ((name, value) in values) {
                val type = BackupSchema.keys[name] ?: continue
                when {
                    type == BackupType.STRING && value is String -> p[stringPreferencesKey(name)] = value
                    type == BackupType.BOOLEAN && value is Boolean -> p[booleanPreferencesKey(name)] = value
                    type == BackupType.INT && value is Int -> p[intPreferencesKey(name)] = value
                }
            }
        }
    }

    // ---- Smart settings and categories ------------------------------------------------------

    suspend fun saveSmartSettings(s: SmartSettings) {
        store.edit { p ->
            p[PrefKeys.SMART_SUGGESTIONS] = s.smartSuggestions
            p[PrefKeys.WEB_SEARCH] = s.webSearchEnabled
            p[PrefKeys.SEARCH_ENGINE] = s.searchEngine.name
            p[PrefKeys.SEARCH_CUSTOM_URL] = s.customSearchUrl.trim().take(300)
            p[PrefKeys.SEARCH_SHORTCUTS] = s.searchSettingsShortcuts
            p[PrefKeys.DRAWER_BY_CATEGORY] = s.groupDrawerByCategory
            p[PrefKeys.SHOW_RECENT_APPS] = s.showRecentApps
        }
    }

    /** Passing a null [category] removes the manual choice so the automatic category applies again. */
    suspend fun setCategoryOverride(pkg: String, category: AppCategory?) {
        store.edit { p ->
            val map = LinkedHashMap(CategoryCodec.decodeOverrides(p[PrefKeys.CATEGORY_OVERRIDES]))
            if (category == null) map.remove(pkg) else map[pkg] = category
            p[PrefKeys.CATEGORY_OVERRIDES] = CategoryCodec.encodeOverrides(map)
        }
    }

    suspend fun saveCategoryConfig(config: CategoryConfig) {
        store.edit { p -> p[PrefKeys.CATEGORY_CONFIG] = CategoryCodec.encodeConfig(config) }
    }

    suspend fun resetCategories() {
        store.edit { p ->
            p.remove(PrefKeys.CATEGORY_CONFIG)
            p.remove(PrefKeys.CATEGORY_OVERRIDES)
        }
    }

    // ---- Security --------------------------------------------------------------------------

    suspend fun setPin(hash: String, salt: String) {
        store.edit { p ->
            p[PrefKeys.PIN_HASH] = hash
            p[PrefKeys.PIN_SALT] = salt
        }
    }

    suspend fun clearPin() {
        store.edit { p ->
            p.remove(PrefKeys.PIN_HASH)
            p.remove(PrefKeys.PIN_SALT)
            p[PrefKeys.BIOMETRIC] = false
        }
    }

    suspend fun saveSecurityOptions(biometric: Boolean, protectSettings: Boolean, protectHidden: Boolean) {
        store.edit { p ->
            p[PrefKeys.BIOMETRIC] = biometric
            p[PrefKeys.PROTECT_SETTINGS] = protectSettings
            p[PrefKeys.PROTECT_HIDDEN] = protectHidden
        }
    }

    // ---- App usage timers ---------------------------------------------------------------------

    suspend fun setTimersEnabled(enabled: Boolean) {
        store.edit { p -> p[PrefKeys.TIMERS_ENABLED] = enabled }
    }

    /** One atomic read-change-write of the timer list, so two quick actions can never overwrite each other. */
    suspend fun updateTimers(transform: (List<AppTimer>) -> List<AppTimer>) {
        store.edit { p ->
            val next = transform(TimerCodec.decode(p[PrefKeys.TIMERS])).take(TimerRules.MAX_TIMERS)
            p[PrefKeys.TIMERS] = TimerCodec.encode(next)
        }
    }

    /**
     * Saves a usage snapshot or a daily roll-over, but only if the timer is still in the period it was read in.
     * A reset by the parent that happened in the meantime is therefore never undone by a late background update.
     */
    suspend fun updateTimerIfSamePeriod(updated: AppTimer, expectedPeriodStartMs: Long, expectedExtraMs: Long) {
        updateTimers { list ->
            list.map {
                if (it.packageName == updated.packageName &&
                    it.periodStartMs == expectedPeriodStartMs && it.extraMs == expectedExtraMs
                ) updated else it
            }
        }
    }

    suspend fun setTimerPin(hash: String, salt: String) {
        store.edit { p ->
            p[PrefKeys.TIMER_PIN_HASH] = hash
            p[PrefKeys.TIMER_PIN_SALT] = salt
            p.remove(PrefKeys.TIMER_PIN_GUARD)
        }
    }

    /** Removing the PIN also switches timers off: without a PIN nobody could authorize more time. */
    suspend fun clearTimerPin() {
        store.edit { p ->
            p.remove(PrefKeys.TIMER_PIN_HASH)
            p.remove(PrefKeys.TIMER_PIN_SALT)
            p.remove(PrefKeys.TIMER_PIN_GUARD)
            p[PrefKeys.TIMER_BIOMETRIC] = false
            p[PrefKeys.TIMERS_ENABLED] = false
        }
    }

    suspend fun setTimerBiometric(enabled: Boolean) {
        store.edit { p -> p[PrefKeys.TIMER_BIOMETRIC] = enabled }
    }

    suspend fun timerGuard(): PinGuard.State = PinGuard.decode(data.first()[PrefKeys.TIMER_PIN_GUARD])

    suspend fun saveTimerGuard(state: PinGuard.State) {
        store.edit { p ->
            if (state == PinGuard.State()) p.remove(PrefKeys.TIMER_PIN_GUARD) else p[PrefKeys.TIMER_PIN_GUARD] = PinGuard.encode(state)
        }
    }

    // ---- Helpers ---------------------------------------------------------------------------

    private suspend fun editList(key: Preferences.Key<String>, transform: (List<String>) -> List<String>) {
        store.edit { p -> p[key] = ListCodec.encode(transform(ListCodec.decode(p[key]))) }
    }

    private suspend fun editFolders(transform: (List<Folder>) -> List<Folder>) {
        store.edit { p -> p[PrefKeys.FOLDERS] = FolderCodec.encode(transform(FolderCodec.decode(p[PrefKeys.FOLDERS]))) }
    }
}
