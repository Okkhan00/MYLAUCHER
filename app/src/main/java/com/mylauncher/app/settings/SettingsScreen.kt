package com.mylauncher.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.ClockStyle
import com.mylauncher.app.data.model.GestureAction
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.launcher.setup.DefaultLauncherController
import com.mylauncher.app.performance.PerformanceMode
import com.mylauncher.app.ui.components.ChoiceRow
import com.mylauncher.app.ui.components.ClickableRow
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SectionTitle
import com.mylauncher.app.ui.components.SwitchRow

private val gestureOptions = listOf(
    GestureAction.OPEN_DRAWER to "App drawer",
    GestureAction.OPEN_SEARCH to "Search",
    GestureAction.QUICK_ACTIONS to "Quick actions",
    GestureAction.OPEN_SETTINGS to "Settings",
    GestureAction.NONE to "Nothing",
)

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
    onOpenWallpaper: () -> Unit,
    onOpenAppLock: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDashboard: () -> Unit,
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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Settings", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SectionTitle("Launcher")
                ClickableRow(
                    title = "Default launcher",
                    subtitle = when {
                        status.isDefault -> "My Launcher is your home app"
                        status.currentLabel != null -> "Current: ${status.currentLabel}"
                        else -> "No default chosen"
                    },
                    onClick = onOpenSetup,
                )

                SectionTitle("Appearance")
                ChoiceRow(
                    "Theme",
                    listOf(
                        ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark",
                        ThemeMode.DYNAMIC to "Dynamic", ThemeMode.CUSTOM to "Custom",
                    ),
                    settings.theme,
                ) { onChange(settings.copy(theme = it)) }
                ClickableRow(
                    title = "Themes and colors",
                    subtitle = "Accent, surfaces, text size, corners, search bar, drawer",
                    onClick = onOpenThemes,
                )
                ClickableRow(
                    title = "Wallpaper",
                    subtitle = "Choose, dim, overlay and blur",
                    onClick = onOpenWallpaper,
                )
                ChoiceRow(
                    "Icon size",
                    listOf(IconSize.SMALL to "Small", IconSize.MEDIUM to "Medium", IconSize.LARGE to "Large"),
                    settings.iconSize,
                ) { onChange(settings.copy(iconSize = it)) }
                ChoiceRow(
                    "Clock",
                    listOf(ClockStyle.DIGITAL to "Digital", ClockStyle.LARGE to "Large digital", ClockStyle.MINIMAL to "Minimal"),
                    settings.clockStyle,
                ) { onChange(settings.copy(clockStyle = it)) }
                SwitchRow("Show date", settings.showDate, { onChange(settings.copy(showDate = it)) })
                SwitchRow("Show app labels", settings.showLabels, { onChange(settings.copy(showLabels = it)) })
                SwitchRow(
                    "Animations", settings.animations, { onChange(settings.copy(animations = it)) },
                    subtitle = "Fade between screens",
                )

                SectionTitle("Home screen")
                ChoiceRow(
                    "Grid columns",
                    listOf(4 to "4", 5 to "5", 6 to "6"),
                    settings.columns,
                ) { onChange(settings.copy(columns = it)) }
                SwitchRow("Search bar", settings.showSearchBar, { onChange(settings.copy(showSearchBar = it)) })
                SwitchRow(
                    "Show battery", settings.showBattery, { onChange(settings.copy(showBattery = it)) },
                    subtitle = "Level and charging state under the clock",
                )

                SectionTitle("Gestures")
                ChoiceRow("Swipe up", gestureOptions, settings.swipeUp) { onChange(settings.copy(swipeUp = it)) }
                ChoiceRow("Swipe down", gestureOptions, settings.swipeDown) { onChange(settings.copy(swipeDown = it)) }
                ChoiceRow("Double tap", gestureOptions, settings.doubleTap) { onChange(settings.copy(doubleTap = it)) }
                Text(
                    "Long press always opens the home edit menu. Locking the screen is not offered " +
                        "because Android only allows it with an Accessibility Service.",
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionTitle("App drawer")
                ChoiceRow(
                    "Layout",
                    listOf(DrawerLayout.GRID to "Grid", DrawerLayout.LIST to "Compact list"),
                    settings.drawerLayout,
                ) { onChange(settings.copy(drawerLayout = it)) }
                ClickableRow(
                    title = "Private apps",
                    subtitle = if (hiddenCount == 0) "None. Hidden inside this launcher only" else "$hiddenCount private",
                    onClick = onOpenHidden,
                )

                SwitchRow(
                    "Remember recently used apps", settings.trackLaunches,
                    { onChange(settings.copy(trackLaunches = it)) },
                    subtitle = "Only apps opened from this launcher, stored on this device",
                )
                ClickableRow(title = "Clear launch history", onClick = onClearHistory)

                SectionTitle("Performance")
                ChoiceRow(
                    "Mode",
                    PerformanceMode.entries.map { it to it.label },
                    settings.performance,
                ) { onChange(settings.copy(performance = it)) }
                Text(
                    settings.performance.description,
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SmartSettingsSection(
                    smart = smart,
                    onChange = onSmartChange,
                    onOpenCategories = onOpenCategories,
                    onOpenInsights = onOpenInsights,
                )

                SectionTitle("Security")
                ClickableRow(
                    title = "Launcher lock",
                    subtitle = if (lockEnabled) "On" else "Off",
                    onClick = onOpenSecurity,
                )
                ClickableRow(
                    title = "App lock",
                    subtitle = when {
                        !lockEnabled -> "Needs a launcher PIN"
                        lockedCount == 0 -> "No locked apps"
                        else -> "$lockedCount locked (only when opened from My Launcher)"
                    },
                    onClick = onOpenAppLock,
                )

                SectionTitle("Tools")
                ClickableRow(
                    title = "Backup and restore",
                    subtitle = "Save or restore your layout and settings as a file",
                    onClick = onOpenBackup,
                )
                ClickableRow(
                    title = "Device dashboard",
                    subtitle = "Battery, storage and system shortcuts",
                    onClick = onOpenDashboard,
                )

                SectionTitle("About")
                Text("My Launcher", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Version $version",
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Private by design: no ads, no analytics, no internet permission. " +
                        "Your layout and settings stay on this device.",
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
