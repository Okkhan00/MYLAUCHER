package com.mylauncher.app.launcher

import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mylauncher.app.MyLauncherApplication
import android.net.Uri
import com.mylauncher.app.backup.BackupCodec
import com.mylauncher.app.backup.BackupResult
import com.mylauncher.app.backup.BackupStatus
import com.mylauncher.app.backup.BackupZip
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LaunchEvent
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.data.model.WallpaperSettings
import com.mylauncher.app.data.model.isFolderToken
import com.mylauncher.app.performance.PerformanceProfile
import com.mylauncher.app.suggestions.SmartSuggestions
import com.mylauncher.app.widgets.HostedWidget
import com.mylauncher.app.launcher.apps.HomeEntry
import com.mylauncher.app.launcher.apps.AppListFilter
import com.mylauncher.app.launcher.apps.LaunchHistory
import com.mylauncher.app.security.pin.PinAttempt
import com.mylauncher.app.security.pin.PinHasher
import com.mylauncher.app.security.pin.PinThrottle
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as MyLauncherApplication).container
    private val prefs = container.preferences
    private val repository = container.appRepository

    /** Null until preferences have loaded, so the UI never flashes the wrong state. */
    val settings: StateFlow<LauncherSettings?> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null until loaded, so a locked launcher is never briefly shown unlocked. */
    val security: StateFlow<SecuritySettings?> =
        prefs.security.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allApps: StateFlow<List<AppInfo>> = repository.apps

    val favoriteIds: StateFlow<List<String>> =
        prefs.favorites.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenIds: StateFlow<Set<String>> =
        prefs.hidden.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val folders: StateFlow<List<Folder>> =
        prefs.folders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Installed apps minus hidden ones: used by home, drawer and search. */
    val visibleApps: StateFlow<List<AppInfo>> =
        combine(allApps, hiddenIds) { apps, hidden -> AppListFilter.visible(apps, hidden) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeEntries: StateFlow<List<HomeEntry>> =
        combine(allApps, favoriteIds, hiddenIds, folders) { apps, tokens, hidden, folderList ->
            HomeEntry.resolve(apps, tokens, hidden, folderList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentApps: StateFlow<List<AppInfo>> =
        combine(prefs.launchStats, visibleApps, settings) { stats, apps, s ->
            if (s?.trackLaunches == true) LaunchHistory.recent(stats, apps) else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mostUsedApps: StateFlow<List<AppInfo>> =
        combine(prefs.launchStats, visibleApps, settings) { stats, apps, s ->
            if (s?.trackLaunches == true) {
                val recent = LaunchHistory.recent(stats, apps)
                LaunchHistory.mostUsed(stats, apps).filter { it !in recent }
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Phase 3A: smart features -----------------------------------------------------------

    val smartSettings: StateFlow<SmartSettings> =
        prefs.smartSettings.stateIn(viewModelScope, SharingStarted.Eagerly, SmartSettings())

    val categoryOverrides: StateFlow<Map<String, AppCategory>> =
        prefs.categoryOverrides.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val categoryConfig: StateFlow<CategoryConfig> =
        prefs.categoryConfig.stateIn(viewModelScope, SharingStarted.Eagerly, CategoryConfig())

    val launchStats: StateFlow<Map<String, LaunchStat>> =
        prefs.launchStats.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** The launch log is empty whenever launch tracking is off, so insights never show hidden data. */
    val launchLog: StateFlow<List<LaunchEvent>> =
        combine(prefs.launchLog, settings) { log, s -> if (s?.trackLaunches == true) log else emptyList() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Bumped when the launcher comes back to the front so time-of-day suggestions refresh. */
    private val suggestionTick = MutableStateFlow(0)

    fun refreshSuggestions() = suggestionTick.update { it + 1 }

    val suggestedApps: StateFlow<List<AppInfo>> =
        combine(launchLog, favoriteIds, visibleApps, smartSettings, suggestionTick) { log, tokens, apps, smart, _ ->
            if (!smart.smartSuggestions) {
                emptyList()
            } else {
                SmartSuggestions.suggest(
                    events = log,
                    favorites = tokens.filterNot(::isFolderToken).toSet(),
                    apps = apps,
                    nowMs = System.currentTimeMillis(),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveSmartSettings(s: SmartSettings) {
        viewModelScope.launch { prefs.saveSmartSettings(s) }
    }

    fun setCategory(app: AppInfo, category: AppCategory?) {
        viewModelScope.launch { prefs.setCategoryOverride(app.packageName, category) }
    }

    fun updateCategoryConfig(transform: (CategoryConfig) -> CategoryConfig) {
        viewModelScope.launch { prefs.saveCategoryConfig(transform(categoryConfig.value)) }
    }

    fun resetCategories() {
        viewModelScope.launch { prefs.resetCategories() }
    }

    // ---- Phase 3B: themes, wallpaper, app lock ----------------------------------------------

    val themeSettings: StateFlow<ThemeSettings?> =
        prefs.themeSettings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val wallpaperSettings: StateFlow<WallpaperSettings> =
        prefs.wallpaperSettings.stateIn(viewModelScope, SharingStarted.Eagerly, WallpaperSettings())

    val lockedApps: StateFlow<Set<String>> =
        prefs.lockedApps.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun saveThemeSettings(t: ThemeSettings) {
        viewModelScope.launch { prefs.saveThemeSettings(t) }
    }

    fun saveWallpaperSettings(w: WallpaperSettings) {
        viewModelScope.launch { prefs.saveWallpaperSettings(w) }
    }

    fun setAppLocked(app: AppInfo, locked: Boolean) {
        viewModelScope.launch { prefs.setAppLocked(app.packageName, locked) }
    }

    // ---- Phase 3C: performance, widgets, backup ----------------------------------------------

    init {
        // Apply the chosen performance mode's cache size and refresh debounce; no polling involved.
        viewModelScope.launch {
            settings.collect { s -> if (s != null) repository.applyProfile(PerformanceProfile.of(s.performance)) }
        }
    }

    val widgetController = container.widgetController

    val hostedWidgets: StateFlow<List<HostedWidget>> =
        widgetController.widgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus

    fun clearBackupStatus() {
        if (_backupStatus.value !is BackupStatus.Working) _backupStatus.value = BackupStatus.Idle
    }

    fun exportBackup(uri: Uri) {
        if (_backupStatus.value is BackupStatus.Working) return
        _backupStatus.value = BackupStatus.Working
        viewModelScope.launch(Dispatchers.IO) {
            _backupStatus.value = try {
                val app = getApplication<Application>()
                val version = try {
                    @Suppress("DEPRECATION")
                    app.packageManager.getPackageInfo(app.packageName, 0).versionName
                } catch (e: Exception) {
                    null
                } ?: "unknown"
                val json = BackupCodec.encode(prefs.exportBackupValues(), System.currentTimeMillis(), version)
                val stream = app.contentResolver.openOutputStream(uri)
                if (stream == null) {
                    BackupStatus.Done("Couldn't write the backup file.", false)
                } else {
                    stream.use { BackupZip.write(it, json) }
                    BackupStatus.Done("Backup saved. It does not contain your PIN.", true)
                }
            } catch (e: Exception) {
                BackupStatus.Done("Couldn't write the backup file.", false)
            }
        }
    }

    fun importBackup(uri: Uri) {
        if (_backupStatus.value is BackupStatus.Working) return
        _backupStatus.value = BackupStatus.Working
        viewModelScope.launch(Dispatchers.IO) {
            _backupStatus.value = try {
                val json = getApplication<Application>().contentResolver.openInputStream(uri)?.use { BackupZip.readJson(it) }
                when (val result = json?.let { BackupCodec.decode(it) }) {
                    is BackupResult.Ok -> {
                        prefs.importBackupValues(result.values)
                        BackupStatus.Done("Backup restored. Your PIN was not changed.", true)
                    }
                    else -> BackupStatus.Done(BackupCodec.USER_MESSAGE, false)
                }
            } catch (e: Exception) {
                BackupStatus.Done(BackupCodec.USER_MESSAGE, false)
            }
        }
    }

    /** Incremented each time the user presses Home while the launcher is open. */
    private val _homeSignal = MutableStateFlow(0)
    val homeSignal: StateFlow<Int> = _homeSignal

    fun onHomePressed() = _homeSignal.update { it + 1 }

    fun peekIcon(app: AppInfo): ImageBitmap? = repository.peekIcon(app)

    suspend fun loadIcon(app: AppInfo): ImageBitmap? = repository.loadIcon(app)

    // ---- Settings --------------------------------------------------------------------------

    fun saveSettings(s: LauncherSettings) {
        viewModelScope.launch { prefs.saveSettings(s) }
    }

    fun completeOnboarding() {
        val current = settings.value ?: LauncherSettings()
        saveSettings(current.copy(onboardingDone = true))
    }

    // ---- Home list -------------------------------------------------------------------------

    fun toggleFavorite(app: AppInfo) {
        viewModelScope.launch {
            if (app.packageName in favoriteIds.value) prefs.removeFavorite(app.packageName)
            else prefs.addFavorite(app.packageName)
        }
    }

    fun moveFavorite(app: AppInfo, delta: Int) {
        viewModelScope.launch { prefs.moveFavorite(app.packageName, delta) }
    }

    fun reorderHome(visibleOrder: List<String>) {
        viewModelScope.launch { prefs.setFavoritesOrder(visibleOrder) }
    }

    fun hideApp(app: AppInfo) {
        viewModelScope.launch { prefs.hide(app.packageName) }
    }

    fun setHidden(app: AppInfo, hidden: Boolean) {
        viewModelScope.launch {
            if (hidden) prefs.hide(app.packageName) else prefs.unhide(app.packageName)
        }
    }

    // ---- Folders ---------------------------------------------------------------------------

    fun createFolder(name: String) {
        viewModelScope.launch { prefs.createFolder(name) }
    }

    fun renameFolder(id: String, name: String) {
        viewModelScope.launch { prefs.renameFolder(id, name) }
    }

    fun deleteFolder(id: String) {
        viewModelScope.launch { prefs.deleteFolder(id) }
    }

    fun addToFolder(id: String, pkg: String) {
        viewModelScope.launch { prefs.addAppToFolder(id, pkg) }
    }

    fun removeFromFolder(id: String, pkg: String) {
        viewModelScope.launch { prefs.removeAppFromFolder(id, pkg) }
    }

    fun moveInFolder(id: String, pkg: String, delta: Int) {
        viewModelScope.launch { prefs.moveAppInFolder(id, pkg, delta) }
    }

    fun reorderFolder(id: String, visibleOrder: List<String>) {
        viewModelScope.launch { prefs.setFolderOrder(id, visibleOrder) }
    }

    // ---- Launch history --------------------------------------------------------------------

    fun recordLaunch(app: AppInfo) {
        if (settings.value?.trackLaunches != true) return
        viewModelScope.launch { prefs.recordLaunch(app.packageName, System.currentTimeMillis()) }
    }

    fun clearLaunchHistory() {
        viewModelScope.launch { prefs.clearLaunchHistory() }
    }

    // ---- Launcher lock ---------------------------------------------------------------------

    private val throttle = PinThrottle()
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked

    fun unlock() {
        _unlocked.value = true
    }

    fun relock() {
        _unlocked.value = false
    }

    suspend fun submitPin(pin: String): PinAttempt {
        val before = SystemClock.elapsedRealtime()
        val remaining = throttle.remainingMs(before)
        if (remaining > 0) return PinAttempt(false, secondsCeil(remaining))
        val sec = security.value ?: return PinAttempt(false, 0)
        val ok = withContext(Dispatchers.Default) {
            PinHasher.verify(pin, sec.pinHash.orEmpty(), sec.pinSalt.orEmpty())
        }
        if (ok) {
            throttle.onSuccess()
            _unlocked.value = true
            return PinAttempt(true, 0)
        }
        val now = SystemClock.elapsedRealtime()
        throttle.onFailure(now)
        return PinAttempt(false, secondsCeil(throttle.remainingMs(now)))
    }

    fun setPin(pin: String) {
        if (!PinHasher.isValidPin(pin)) return
        viewModelScope.launch(Dispatchers.Default) {
            val hashed = PinHasher.hash(pin)
            prefs.setPin(hashed.hash, hashed.salt)
            _unlocked.value = true
        }
    }

    fun removePin() {
        viewModelScope.launch { prefs.clearPin() }
    }

    fun saveSecurityOptions(biometric: Boolean, protectSettings: Boolean, protectHidden: Boolean) {
        viewModelScope.launch { prefs.saveSecurityOptions(biometric, protectSettings, protectHidden) }
    }

    private fun secondsCeil(ms: Long): Int = ceil(ms / 1000.0).toInt()
}
