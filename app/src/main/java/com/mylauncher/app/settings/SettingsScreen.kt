package com.mylauncher.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.ClockStyle
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.GestureAction
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.launcher.setup.DefaultLauncherController
import com.mylauncher.app.performance.PerformanceMode
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SettingChoice
import com.mylauncher.app.ui.components.SettingDivider
import com.mylauncher.app.ui.components.SettingInfo
import com.mylauncher.app.ui.components.SettingNote
import com.mylauncher.app.ui.components.SettingRow
import com.mylauncher.app.ui.components.SettingSwitch
import com.mylauncher.app.ui.components.SettingsGroup

private val gestureOptions = listOf(
    GestureAction.OPEN_DRAWER to "App drawer",
    GestureAction.OPEN_SEARCH to "Search",
    GestureAction.QUICK_ACTIONS to "Quick actions",
    GestureAction.OPEN_SETTINGS to "Settings",
    GestureAction.NONE to "Nothing",
)

/**
 * Settings, grouped by what the user wants to change. Every control reads and writes the same saved
 * values as before (nothing is reset by opening this screen); only the layout is new.
 */
@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onChange: (LauncherSettings) -> Unit,
    controller: DefaultLauncherController,
    hiddenCount: Int,
    lockEnabled: Boolean,
    onBack: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenSecurity: () -> Unit,
    onClearHistory: () -> Unit,
    smart: SmartSettings,
    onSmartChange: (SmartSettings) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenInsights: () -> Unit,
    lockedCount: Int,
    onOpenThemes: () -> Unit,
    themeName: String,
    onOpenWallpaper: () -> Unit,
    onOpenAppLock: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDashboard: () -> Unit,
    timerSummary: String,
    onOpenTimer: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        } ?: "unknown"
    }
    val status = controller.status
    var confirmClearHistory by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Settings", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Text(
                    "Change how My Launcher looks and works. Changes apply straight away.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsGroup("Home screen", "🏠") {
                    SettingChoice(
                        "Apps per row",
                        listOf(4 to "4", 5 to "5", 6 to "6"),
                        settings.columns,
                        { onChange(settings.copy(columns = it)) },
                        subtitle = "On the home screen and in the app drawer",
                    )
                    SettingDivider()
                    SettingChoice(
                        "Icon size",
                        listOf(IconSize.SMALL to "Small", IconSize.MEDIUM to "Medium", IconSize.LARGE to "Large"),
                        settings.iconSize,
                        { onChange(settings.copy(iconSize = it)) },
                    )
                    SettingDivider()
                    SettingSwitch(
                        "Show app names", settings.showLabels, { onChange(settings.copy(showLabels = it)) },
                        subtitle = "Names under icons on the home screen and in the drawer",
                    )
                    SettingDivider()
                    SettingChoice(
                        "Clock style",
                        listOf(ClockStyle.DIGITAL to "Digital", ClockStyle.LARGE to "Large", ClockStyle.MINIMAL to "Minimal"),
                        settings.clockStyle,
                        { onChange(settings.copy(clockStyle = it)) },
                    )
                    SettingDivider()
                    SettingSwitch("Show date", settings.showDate, { onChange(settings.copy(showDate = it)) })
                    SettingDivider()
                    SettingSwitch("Show search bar", settings.showSearchBar, { onChange(settings.copy(showSearchBar = it)) })
                    SettingDivider()
                    SettingSwitch(
                        "Show battery", settings.showBattery, { onChange(settings.copy(showBattery = it)) },
                        subtitle = "Battery level and charging state under the clock",
                    )
                }

                SettingsGroup("App drawer", "📱") {
                    SettingChoice(
                        "Open the drawer in",
                        listOf(true to "Categories", false to "All Apps"),
                        smart.groupDrawerByCategory,
                        { onSmartChange(smart.copy(groupDrawerByCategory = it)) },
                        subtitle = "You can also switch at the top of the drawer",
                    )
                    SettingDivider()
                    SettingChoice(
                        "Layout",
                        listOf(DrawerLayout.GRID to "Grid", DrawerLayout.LIST to "List"),
                        settings.drawerLayout,
                        { onChange(settings.copy(drawerLayout = it)) },
                        subtitle = "For All Apps and inside a category",
                    )
                    SettingDivider()
                    SettingSwitch(
                        "Suggested apps", smart.smartSuggestions, { onSmartChange(smart.copy(smartSuggestions = it)) },
                        subtitle = "Suggest apps by time of day. Uses only data on this device.",
                    )
                    SettingDivider()
                    SettingRow(
                        "Manage categories",
                        onClick = onOpenCategories,
                        subtitle = "Rename, hide and reorder categories, or move apps between them",
                    )
                }

                SearchSettingsGroup(smart = smart, onChange = onSmartChange)

                SettingsGroup("Gestures", "👆") {
                    SettingChoice("Swipe up", gestureOptions, settings.swipeUp, { onChange(settings.copy(swipeUp = it)) })
                    SettingDivider()
                    SettingChoice("Swipe down", gestureOptions, settings.swipeDown, { onChange(settings.copy(swipeDown = it)) })
                    SettingDivider()
                    SettingChoice("Double tap", gestureOptions, settings.doubleTap, { onChange(settings.copy(doubleTap = it)) })
                }
                SettingNote(
                    "Long press on the home screen always opens the edit menu. Locking the screen is not " +
                        "offered because Android only allows it with an Accessibility Service.",
                )

                SettingsGroup("Appearance", "🎨") {
                    SettingRow(
                        "Themes",
                        onClick = onOpenThemes,
                        subtitle = "Ready-made themes, your own colors, text size and corners",
                        value = themeName,
                    )
                    SettingDivider()
                    SettingRow("Wallpaper", onClick = onOpenWallpaper, subtitle = "Choose a wallpaper, dim it or add a blur")
                    SettingDivider()
                    SettingSwitch(
                        "Animations", settings.animations, { onChange(settings.copy(animations = it)) },
                        subtitle = "Smooth fades between screens",
                    )
                }

                SettingsGroup("Privacy and hidden apps", "🔒") {
                    SettingRow(
                        "Private apps",
                        onClick = onOpenHidden,
                        subtitle = "Hidden inside this launcher only",
                        value = if (hiddenCount == 0) "None" else "$hiddenCount",
                    )
                    SettingDivider()
                    SettingRow(
                        "Launcher lock",
                        onClick = onOpenSecurity,
                        subtitle = "Protect settings and private apps with a PIN or fingerprint",
                        value = if (lockEnabled) "On" else "Off",
                    )
                    SettingDivider()
                    SettingRow(
                        "App lock",
                        onClick = onOpenAppLock,
                        subtitle = when {
                            !lockEnabled -> "Needs a launcher PIN first"
                            else -> "Ask for your PIN when opening chosen apps from My Launcher"
                        },
                        value = if (lockedCount == 0) null else "$lockedCount",
                    )
                    SettingDivider()
                    SettingRow(
                        "App usage timer",
                        onClick = onOpenTimer,
                        subtitle = "Daily time limits for chosen apps, unlocked with a separate parent PIN",
                        value = timerSummary,
                    )
                    SettingDivider()
                    SettingSwitch(
                        "Remember recently used apps", settings.trackLaunches,
                        { onChange(settings.copy(trackLaunches = it)) },
                        subtitle = "Only apps opened from this launcher, stored on this device",
                    )
                    SettingDivider()
                    SettingRow(
                        "Usage insights",
                        onClick = onOpenInsights,
                        subtitle = "Most used and recently opened apps",
                    )
                    SettingDivider()
                    SettingRow(
                        "Clear usage history",
                        onClick = { confirmClearHistory = true },
                        subtitle = "Removes recent apps, most used apps and insights",
                    )
                }

                SettingsGroup("General", "⚙️") {
                    SettingRow(
                        "Default launcher",
                        onClick = onOpenSetup,
                        subtitle = when {
                            status.isDefault -> "My Launcher is your home app"
                            status.currentLabel != null -> "Your home app is ${status.currentLabel}"
                            else -> "No home app chosen yet"
                        },
                    )
                    SettingDivider()
                    SettingChoice(
                        "Performance",
                        PerformanceMode.entries.map { it to it.label },
                        settings.performance,
                        { onChange(settings.copy(performance = it)) },
                        subtitle = settings.performance.description,
                    )
                    SettingDivider()
                    SettingRow(
                        "Backup and restore",
                        onClick = onOpenBackup,
                        subtitle = "Save or restore your layout and settings as a file",
                    )
                    SettingDivider()
                    SettingRow(
                        "Device dashboard",
                        onClick = onOpenDashboard,
                        subtitle = "Battery, storage and system shortcuts",
                    )
                }

                SettingsGroup("About", "ℹ️") {
                    SettingInfo("My Launcher", "Version $version · Azi Creation")
                    SettingDivider()
                    SettingRow(
                        "Show the welcome guide again",
                        onClick = { onChange(settings.copy(onboardingDone = false)) },
                        subtitle = "Replays the short first-time setup",
                    )
                }
                SettingNote(
                    "Private by design: no ads, no analytics, no internet permission. " +
                        "Your layout and settings stay on this device.",
                )
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear usage history?") },
            text = { Text("This removes recent apps, most used apps, suggestions data and insights. It cannot be undone.") },
            confirmButton = { TextButton(onClick = { onClearHistory(); confirmClearHistory = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } },
        )
    }
}
