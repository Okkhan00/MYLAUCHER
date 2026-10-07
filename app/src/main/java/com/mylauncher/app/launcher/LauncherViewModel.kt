package com.mylauncher.app.launcher

import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mylauncher.app.MyLauncherApplication
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SecuritySettings
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
