package com.mylauncher.app.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mylauncher.app.backup.BackupScreen
import com.mylauncher.app.categories.CategoryOrganizer
import com.mylauncher.app.device.DeviceDashboardScreen
import com.mylauncher.app.performance.PerformanceProfile
import com.mylauncher.app.widgets.HomeWidgets
import com.mylauncher.app.widgets.WidgetHostLifecycle
import com.mylauncher.app.widgets.WidgetPickerDialog
import com.mylauncher.app.widgets.rememberAddWidget
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.launcher.appdrawer.DrawerScreen
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.folders.FolderActions
import com.mylauncher.app.insights.UsageInsightsScreen
import com.mylauncher.app.launcher.home.HomeScreen
import com.mylauncher.app.launcher.search.SystemAction
import com.mylauncher.app.privacy.AppLockPolicy
import com.mylauncher.app.privacy.AppLockScreen
import com.mylauncher.app.privacy.PrivateSpaceScreen
import com.mylauncher.app.settings.ThemeScreen
import com.mylauncher.app.wallpaper.WallpaperOverlay
import com.mylauncher.app.wallpaper.WallpaperScreen
import com.mylauncher.app.launcher.search.WebSearch
import com.mylauncher.app.launcher.setup.SetupScreen
import com.mylauncher.app.launcher.setup.rememberDefaultLauncherController
import com.mylauncher.app.security.LockScreen
import com.mylauncher.app.security.biometric.BiometricHelper
import com.mylauncher.app.settings.CategoriesScreen
import com.mylauncher.app.settings.HiddenAppsScreen
import com.mylauncher.app.settings.SecurityScreen
import com.mylauncher.app.settings.SettingsScreen
import com.mylauncher.app.ui.components.IconSource
import com.mylauncher.app.ui.components.LocalIconSource
import com.mylauncher.app.ui.theme.MyLauncherTheme

enum class Screen(val parent: Screen?) {
    HOME(null),
    DRAWER(HOME),
    SETTINGS(HOME),
    HIDDEN(SETTINGS),
    SETUP(SETTINGS),
    SECURITY(SETTINGS),
    CATEGORIES(SETTINGS),
    INSIGHTS(SETTINGS),
    THEMES(SETTINGS),
    WALLPAPER(SETTINGS),
    APP_LOCK(SETTINGS),
    BACKUP(SETTINGS),
    DASHBOARD(SETTINGS),
    PRIVATE(DRAWER),
}

/** Which screens the launcher lock covers. Home, drawer and search are never locked. */
internal fun requiresUnlock(screen: Screen, security: SecuritySettings): Boolean {
    if (!security.lockEnabled) return false
    return when (screen) {
        Screen.SETTINGS, Screen.SETUP, Screen.CATEGORIES, Screen.INSIGHTS,
        Screen.THEMES, Screen.WALLPAPER, Screen.APP_LOCK, Screen.BACKUP, Screen.DASHBOARD -> security.protectSettings
        Screen.HIDDEN, Screen.PRIVATE -> security.protectHidden
        Screen.SECURITY -> true
        Screen.HOME, Screen.DRAWER -> false
    }
}

@Composable
fun LauncherApp(vm: LauncherViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val security by vm.security.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val visibleApps by vm.visibleApps.collectAsStateWithLifecycle()
    val allApps by vm.allApps.collectAsStateWithLifecycle()
    val entries by vm.homeEntries.collectAsStateWithLifecycle()
    val favoriteIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val hiddenIds by vm.hiddenIds.collectAsStateWithLifecycle()
    val recentApps by vm.recentApps.collectAsStateWithLifecycle()
    val mostUsedApps by vm.mostUsedApps.collectAsStateWithLifecycle()
    val homeSignal by vm.homeSignal.collectAsStateWithLifecycle()
    val smart by vm.smartSettings.collectAsStateWithLifecycle()
    val suggestedApps by vm.suggestedApps.collectAsStateWithLifecycle()
    val launchStats by vm.launchStats.collectAsStateWithLifecycle()
    val launchLog by vm.launchLog.collectAsStateWithLifecycle()
    val categoryOverrides by vm.categoryOverrides.collectAsStateWithLifecycle()
    val categoryConfig by vm.categoryConfig.collectAsStateWithLifecycle()
    val themeSettings by vm.themeSettings.collectAsStateWithLifecycle()
    val wallpaper by vm.wallpaperSettings.collectAsStateWithLifecycle()
    val lockedApps by vm.lockedApps.collectAsStateWithLifecycle()
    val backupStatus by vm.backupStatus.collectAsStateWithLifecycle()
    val hostedWidgets by vm.hostedWidgets.collectAsStateWithLifecycle()
    val widgetController = vm.widgetController
    var showWidgetPicker by remember { mutableStateOf(false) }

    // App waiting for PIN/biometric confirmation before it opens. Never saved with instance state.
    var pendingLaunch by remember { mutableStateOf<AppInfo?>(null) }
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    var handledSignal by rememberSaveable { mutableIntStateOf(homeSignal) }

    // Pressing Home while the launcher is open always returns to the home screen.
    LaunchedEffect(homeSignal) {
        if (homeSignal != handledSignal) {
            handledSignal = homeSignal
            pendingLaunch = null
            screen = Screen.HOME
        }
    }

    // The unlock only lasts while the user stays inside settings: relock on home/drawer or when the app is hidden.
    LaunchedEffect(screen) {
        if (screen == Screen.HOME || screen == Screen.DRAWER) vm.relock()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.relock(); pendingLaunch = null }
    // Time-of-day suggestions are recomputed whenever the launcher returns to the front.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refreshSuggestions() }

    // Only grouped when the Categories view is on, so the default drawer does no extra work.
    val categoryGroups = remember(visibleApps, categoryOverrides, categoryConfig, smart.groupDrawerByCategory) {
        if (smart.groupDrawerByCategory) CategoryOrganizer.group(visibleApps, categoryOverrides, categoryConfig) else emptyList()
    }

    val controller = rememberDefaultLauncherController()
    val biometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val iconSource = remember(vm) { IconSource(peek = vm::peekIcon, load = vm::loadIcon) }
    val folderActions = FolderActions(
        create = vm::createFolder,
        rename = vm::renameFolder,
        delete = vm::deleteFolder,
        addApp = vm::addToFolder,
        removeApp = vm::removeFromFolder,
        reorder = vm::reorderFolder,
    )

    val s: LauncherSettings = settings ?: return
    val sec: SecuritySettings = security ?: return
    val ts = themeSettings ?: return

    val wallpaperScrim = WallpaperOverlay.color(wallpaper)
    val drawerOpacity = ts.drawerOpacity / 100f
    val profile = PerformanceProfile.of(s.performance)
    val allowEffects = profile.allowEffects
    // Battery Saver switches animations off for the launcher's own screens without changing the saved setting.
    val view = s.copy(animations = s.animations && profile.allowAnimations)

    MyLauncherTheme(
        mode = s.theme,
        homeVisible = s.onboardingDone && (screen == Screen.HOME || screen == Screen.WALLPAPER),
        themeSettings = ts,
    ) {
        CompositionLocalProvider(LocalIconSource provides iconSource) {
            BackHandler(enabled = s.onboardingDone && screen.parent != null) {
                screen.parent?.let { screen = it }
            }

            val doLaunch: (AppInfo) -> Unit = { app ->
                vm.recordLaunch(app)
                AppActions.launch(context, app)
                screen = Screen.HOME
            }
            // Every launch from the launcher goes through here, so App Lock covers home, drawer,
            // folders, search and private apps alike.
            val launchApp: (AppInfo) -> Unit = { app ->
                if (AppLockPolicy.requiresAuth(app.packageName, sec, lockedApps)) pendingLaunch = app else doLaunch(app)
            }

            val menu = AppMenuActions(
                favoriteIds = favoriteIds,
                folders = folders,
                toggleFavorite = vm::toggleFavorite,
                move = vm::moveFavorite,
                hide = vm::hideApp,
                addToFolder = { app, folderId -> vm.addToFolder(folderId, app.packageName) },
                open = launchApp,
                removeFromFolder = { app, folderId -> vm.removeFromFolder(folderId, app.packageName) },
                privateApps = hiddenIds,
                setPrivate = vm::setHidden,
                appLockAvailable = sec.lockEnabled,
                lockedApps = lockedApps,
                setLocked = vm::setAppLocked,
                categories = categoryConfig.normalized().entries,
                categoryOf = { app -> CategoryOrganizer.categoryOf(app, categoryOverrides) },
                setCategory = vm::setCategory,
            )

            WidgetHostLifecycle(widgetController)
            val addWidget = rememberAddWidget(widgetController)
            if (showWidgetPicker) {
                WidgetPickerDialog(widgetController, onPick = addWidget, onDismiss = { showWidgetPicker = false })
            }

            val content: @Composable (Screen) -> Unit = { target ->
                val waiting = pendingLaunch
                if (waiting != null) {
                    LockScreen(
                        biometricEnabled = sec.biometricEnabled,
                        biometricAvailable = biometricAvailable,
                        submitPin = { pin ->
                            val attempt = vm.submitPin(pin)
                            if (attempt.success) {
                                vm.relock() // unlocking one app must not unlock settings
                                pendingLaunch = null
                                doLaunch(waiting)
                            }
                            attempt
                        },
                        onBiometricSuccess = { pendingLaunch = null; doLaunch(waiting) },
                        onCancel = { pendingLaunch = null },
                        title = "Unlock ${waiting.label}",
                        message = "This app is protected by App Lock. Enter your launcher PIN to open it.",
                        cancelLabel = "Cancel",
                    )
                } else if (requiresUnlock(target, sec) && !unlocked) {
                    LockScreen(
                        biometricEnabled = sec.biometricEnabled,
                        biometricAvailable = biometricAvailable,
                        submitPin = vm::submitPin,
                        onBiometricSuccess = vm::unlock,
                        onCancel = { screen = Screen.HOME },
                    )
                } else {
                    when (target) {
                        Screen.HOME -> HomeScreen(
                            settings = view,
                            entries = entries,
                            selectableApps = visibleApps,
                            menu = menu,
                            folderActions = folderActions,
                            onOpenDrawer = { focusSearch = false; screen = Screen.DRAWER },
                            onOpenSearch = { focusSearch = true; screen = Screen.DRAWER },
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onLaunch = launchApp,
                            onReorderHome = vm::reorderHome,
                            scrim = wallpaperScrim,
                            widgetsContent = if (hostedWidgets.isNotEmpty()) {
                                { modifier -> HomeWidgets(widgetController, hostedWidgets, modifier) }
                            } else {
                                null
                            },
                            onAddWidget = { showWidgetPicker = true },
                        )
                        Screen.DRAWER -> DrawerScreen(
                            apps = visibleApps,
                            settings = view,
                            focusSearch = focusSearch,
                            recentApps = recentApps,
                            mostUsedApps = mostUsedApps,
                            suggestedApps = suggestedApps,
                            folders = folders,
                            launchStats = launchStats,
                            smart = smart,
                            categoryGroups = categoryGroups,
                            privateCount = hiddenIds.size,
                            onOpenPrivate = { screen = Screen.PRIVATE },
                            drawerOpacity = drawerOpacity,
                            wallpaperScrim = if (wallpaper.drawerOverlay && drawerOpacity < 1f) wallpaperScrim else Color.Transparent,
                            menu = menu,
                            onLaunch = launchApp,
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onGroupByCategoryChange = { vm.saveSmartSettings(smart.copy(groupDrawerByCategory = it)) },
                            onSettingShortcut = { AppActions.openSettingsAction(context, it.intentAction, it.title) },
                            onSystemAction = { action ->
                                when (action) {
                                    SystemAction.LAUNCHER_SETTINGS -> screen = Screen.SETTINGS
                                    SystemAction.WALLPAPER -> screen = Screen.WALLPAPER
                                    SystemAction.USAGE_INSIGHTS -> screen = Screen.INSIGHTS
                                    SystemAction.APP_CATEGORIES -> screen = Screen.CATEGORIES
                                    SystemAction.PRIVATE_APPS -> screen = Screen.PRIVATE
                                    SystemAction.DEVICE_DASHBOARD -> screen = Screen.DASHBOARD
                                    SystemAction.BACKUP -> screen = Screen.BACKUP
                                }
                            },
                            onWebSearch = { query ->
                                val url = WebSearch.buildUrl(smart.searchEngine, smart.customSearchUrl, query)
                                if (url != null) {
                                    AppActions.openWebSearch(context, url)
                                } else {
                                    Toast.makeText(context, "Set a valid custom search address in Settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            settings = s,
                            onChange = vm::saveSettings,
                            controller = controller,
                            hiddenCount = hiddenIds.size,
                            lockEnabled = sec.lockEnabled,
                            onBack = { screen = Screen.HOME },
                            onOpenHidden = { screen = Screen.HIDDEN },
                            onOpenSetup = { screen = Screen.SETUP },
                            onOpenSecurity = { screen = Screen.SECURITY },
                            onClearHistory = vm::clearLaunchHistory,
                            smart = smart,
                            onSmartChange = vm::saveSmartSettings,
                            onOpenCategories = { screen = Screen.CATEGORIES },
                            onOpenInsights = { screen = Screen.INSIGHTS },
                            lockedCount = lockedApps.size,
                            onOpenThemes = { screen = Screen.THEMES },
                            onOpenWallpaper = { screen = Screen.WALLPAPER },
                            onOpenAppLock = { screen = Screen.APP_LOCK },
                            onOpenBackup = { screen = Screen.BACKUP },
                            onOpenDashboard = { screen = Screen.DASHBOARD },
                        )
                        Screen.BACKUP -> BackupScreen(
                            status = backupStatus,
                            onExport = vm::exportBackup,
                            onImport = vm::importBackup,
                            onClearStatus = vm::clearBackupStatus,
                            onBack = { vm.clearBackupStatus(); screen = Screen.SETTINGS },
                        )
                        Screen.DASHBOARD -> DeviceDashboardScreen(onBack = { screen = Screen.SETTINGS })
                        Screen.THEMES -> ThemeScreen(
                            settings = s,
                            theme = ts,
                            onSettingsChange = vm::saveSettings,
                            onThemeChange = vm::saveThemeSettings,
                            onBack = { screen = Screen.SETTINGS },
                        )
                        Screen.WALLPAPER -> WallpaperScreen(
                            settings = wallpaper,
                            allowEffects = allowEffects,
                            onChange = vm::saveWallpaperSettings,
                            onBack = { screen = Screen.SETTINGS },
                        )
                        Screen.APP_LOCK -> AppLockScreen(
                            apps = allApps,
                            lockedApps = lockedApps,
                            pinSet = sec.lockEnabled,
                            onSetLocked = vm::setAppLocked,
                            onOpenSecurity = { screen = Screen.SECURITY },
                            onBack = { screen = Screen.SETTINGS },
                        )
                        Screen.PRIVATE -> PrivateSpaceScreen(
                            privateApps = allApps.filter { it.packageName in hiddenIds },
                            lockActive = sec.lockEnabled,
                            iconSizeDp = s.iconSize.sizeDp,
                            menu = menu,
                            onLaunch = launchApp,
                            onBack = { screen = Screen.DRAWER },
                        )
                        Screen.CATEGORIES -> CategoriesScreen(
                            apps = visibleApps,
                            overrides = categoryOverrides,
                            config = categoryConfig,
                            onConfigChange = vm::updateCategoryConfig,
                            onSetCategory = vm::setCategory,
                            onReset = vm::resetCategories,
                            onBack = { screen = Screen.SETTINGS },
                        )
                        Screen.INSIGHTS -> UsageInsightsScreen(
                            events = launchLog,
                            apps = visibleApps,
                            trackingEnabled = s.trackLaunches,
                            onTrackingChange = { vm.saveSettings(s.copy(trackLaunches = it)) },
                            onClearHistory = vm::clearLaunchHistory,
                            onBack = { screen = Screen.SETTINGS },
                        )
                        Screen.HIDDEN -> HiddenAppsScreen(
                            apps = allApps,
                            hiddenIds = hiddenIds,
                            onSetHidden = vm::setHidden,
                            onBack = { screen = Screen.SETTINGS },
                        )
                        Screen.SETUP -> SetupScreen(controller = controller, onBack = { screen = Screen.SETTINGS })
                        Screen.SECURITY -> SecurityScreen(
                            security = sec,
                            biometricAvailable = biometricAvailable,
                            onSetPin = vm::setPin,
                            onRemovePin = vm::removePin,
                            onOptionsChange = vm::saveSecurityOptions,
                            onBack = { screen = Screen.SETTINGS },
                        )
                    }
                }
            }

            if (!s.onboardingDone) {
                OnboardingScreen(
                    settings = s,
                    controller = controller,
                    onThemeChange = { vm.saveSettings(s.copy(theme = it)) },
                    onFinish = vm::completeOnboarding,
                )
            } else if (view.animations) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                    label = "screen",
                ) { target -> content(target) }
            } else {
                content(screen)
            }
        }
    }
}
