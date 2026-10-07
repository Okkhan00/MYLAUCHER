package com.mylauncher.app.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.launcher.appdrawer.DrawerScreen
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.folders.FolderActions
import com.mylauncher.app.launcher.home.HomeScreen
import com.mylauncher.app.launcher.setup.SetupScreen
import com.mylauncher.app.launcher.setup.rememberDefaultLauncherController
import com.mylauncher.app.security.LockScreen
import com.mylauncher.app.security.biometric.BiometricHelper
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
}

/** Which screens the launcher lock covers. Home, drawer and search are never locked. */
internal fun requiresUnlock(screen: Screen, security: SecuritySettings): Boolean {
    if (!security.lockEnabled) return false
    return when (screen) {
        Screen.SETTINGS, Screen.SETUP -> security.protectSettings
        Screen.HIDDEN -> security.protectHidden
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

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var focusSearch by rememberSaveable { mutableStateOf(false) }
    var handledSignal by rememberSaveable { mutableIntStateOf(homeSignal) }

    // Pressing Home while the launcher is open always returns to the home screen.
    LaunchedEffect(homeSignal) {
        if (homeSignal != handledSignal) {
            handledSignal = homeSignal
            screen = Screen.HOME
        }
    }

    // The unlock only lasts while the user stays inside settings: relock on home/drawer or when the app is hidden.
    LaunchedEffect(screen) {
        if (screen == Screen.HOME || screen == Screen.DRAWER) vm.relock()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.relock() }

    val controller = rememberDefaultLauncherController()
    val biometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val iconSource = remember(vm) { IconSource(peek = vm::peekIcon, load = vm::loadIcon) }
    val menu = AppMenuActions(
        favoriteIds = favoriteIds,
        folders = folders,
        toggleFavorite = vm::toggleFavorite,
        move = vm::moveFavorite,
        hide = vm::hideApp,
        addToFolder = { app, folderId -> vm.addToFolder(folderId, app.packageName) },
    )
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

    MyLauncherTheme(mode = s.theme, homeVisible = s.onboardingDone && screen == Screen.HOME) {
        CompositionLocalProvider(LocalIconSource provides iconSource) {
            BackHandler(enabled = s.onboardingDone && screen.parent != null) {
                screen.parent?.let { screen = it }
            }

            val launchApp: (AppInfo) -> Unit = { app ->
                vm.recordLaunch(app)
                AppActions.launch(context, app)
                screen = Screen.HOME
            }

            val content: @Composable (Screen) -> Unit = { target ->
                if (requiresUnlock(target, sec) && !unlocked) {
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
                            settings = s,
                            entries = entries,
                            selectableApps = visibleApps,
                            menu = menu,
                            folderActions = folderActions,
                            onOpenDrawer = { focusSearch = false; screen = Screen.DRAWER },
                            onOpenSearch = { focusSearch = true; screen = Screen.DRAWER },
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onLaunch = launchApp,
                            onReorderHome = vm::reorderHome,
                        )
                        Screen.DRAWER -> DrawerScreen(
                            apps = visibleApps,
                            settings = s,
                            focusSearch = focusSearch,
                            recentApps = recentApps,
                            mostUsedApps = mostUsedApps,
                            menu = menu,
                            onLaunch = launchApp,
                            onOpenSettings = { screen = Screen.SETTINGS },
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
            } else if (s.animations) {
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
