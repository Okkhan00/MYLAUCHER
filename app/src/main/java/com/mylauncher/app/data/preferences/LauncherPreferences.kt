package com.mylauncher.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mylauncher.app.data.model.ClockStyle
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.GestureAction
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.model.folderToken
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
            )
        }
        .catch { emit(LauncherSettings()) }

    /** Ordered home list: package names and "folder:<id>" tokens. */
    val favorites: Flow<List<String>> = data.map { ListCodec.decode(it[PrefKeys.FAVORITES]) }

    val hidden: Flow<Set<String>> = data.map { ListCodec.decode(it[PrefKeys.HIDDEN]).toSet() }

    val folders: Flow<List<Folder>> = data.map { FolderCodec.decode(it[PrefKeys.FOLDERS]) }

    val launchStats: Flow<Map<String, LaunchStat>> = data.map { LaunchStatsCodec.decode(it[PrefKeys.LAUNCH_STATS]) }

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
        }
    }

    suspend fun clearLaunchHistory() {
        store.edit { it.remove(PrefKeys.LAUNCH_STATS) }
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

    // ---- Helpers ---------------------------------------------------------------------------

    private suspend fun editList(key: Preferences.Key<String>, transform: (List<String>) -> List<String>) {
        store.edit { p -> p[key] = ListCodec.encode(transform(ListCodec.decode(p[key]))) }
    }

    private suspend fun editFolders(transform: (List<Folder>) -> List<Folder>) {
        store.edit { p -> p[PrefKeys.FOLDERS] = FolderCodec.encode(transform(FolderCodec.decode(p[PrefKeys.FOLDERS]))) }
    }
}
