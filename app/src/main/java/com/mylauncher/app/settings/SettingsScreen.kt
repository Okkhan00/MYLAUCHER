package com.mylauncher.app.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mylauncher.app.about.VersionHistory
import com.mylauncher.app.data.model.ClockStyle
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.GestureAction
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.launcher.setup.DefaultLauncherController
import com.mylauncher.app.performance.PerformanceMode
import com.mylauncher.app.ui.components.IconBadge
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
    onOpenCustomizeHome: () -> Unit,
    onOpenCustomizeDrawer: () -> Unit,
    /** Null shows the dashboard of categories; otherwise the page of that category. */
    section: SettingsSection?,
    onOpenSection: (SettingsSection) -> Unit,
    /** One scroll state per category page, owned by the launcher so positions survive opening a sub-screen. */
    sectionScrolls: List<ScrollState>,
    widgetCount: Int,
    onAddWidget: () -> Unit,
    onOpenIconPacks: () -> Unit,
    onOpenCustomIcons: () -> Unit,
    iconPackSummary: String,
    customIconCount: Int,
    /** Owned by the launcher so the scroll position survives opening a section and coming back. */
    scrollState: ScrollState,
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
    var showHistory by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader(section?.title ?: "Settings", onBack)
            val activeScroll = if (section == null) scrollState else sectionScrolls[section.ordinal]
            Column(Modifier.weight(1f).verticalScroll(activeScroll).padding(bottom = 24.dp)) {
                if (section == null) {
                    Text(
                        "Change how My Launcher looks and works. Changes apply straight away.",
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SettingsSection.entries.forEach { entry ->
                        SectionCard(
                            entry,
                            summary = when (entry) {
                                SettingsSection.HOME -> "${settings.homeColumns} per row" + if (settings.dockCount > 0) ", dock of ${settings.dockCount}" else ""
                                SettingsSection.DRAWER -> if (settings.drawerLayout == DrawerLayout.LIST) "List" else "${settings.columns} per row"
                                SettingsSection.WIDGETS -> if (widgetCount == 0) "None added" else "$widgetCount added"
                                SettingsSection.APPEARANCE -> themeName
                                SettingsSection.ICONS -> iconPackSummary
                                SettingsSection.BEHAVIOR -> settings.performance.label
                                SettingsSection.PRIVACY -> if (lockEnabled) "Lock on" else "Lock off"
                                SettingsSection.MORE -> "Version $version"
                            },
                            onClick = { onOpenSection(entry) },
                        )
                    }
                }

                if (section == SettingsSection.WIDGETS) {
                    SettingsGroup("Widgets", "🧩") {
                        SettingRow(
                            "Add a widget",
                            onClick = onAddWidget,
                            subtitle = "Opens the list of widgets from your installed apps. Android may ask you to allow it.",
                        )
                        SettingDivider()
                        SettingInfo(
                            "On your home screen",
                            if (widgetCount == 0) "No widgets yet." else "$widgetCount widgets. Use the menu on a widget to move, resize or remove it.",
                        )
                    }
                    SettingNote(
                        "Widgets are provided by other apps and run inside My Launcher. Whether a widget works, and how it " +
                            "can be resized, depends on that app and on your Android version.",
                    )
                }
                if (section == SettingsSection.ICONS) {
                    SettingsGroup("Icons", "🎭") {
                        SettingRow(
                            "Icon packs",
                            onClick = onOpenIconPacks,
                            subtitle = "Use an installed icon pack for every app icon",
                            value = iconPackSummary,
                        )
                        SettingDivider()
                        SettingRow(
                            "Custom icons",
                            onClick = onOpenCustomIcons,
                            subtitle = "Make your own icon for any app from a picture",
                            value = if (customIconCount == 0) null else "$customIconCount",
                        )
                    }
                    SettingNote("Order: your custom icon first, then the icon pack, then the app's own icon.")
                }

                if (section == SettingsSection.APPEARANCE) {
                    SettingsGroup("Personalization and themes", "🎨") {
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

                }
                if (section == SettingsSection.HOME) {
                    SettingsGroup("Home screen", "🏠") {
                        SettingRow(
                            "Customize home screen",
                            onClick = onOpenCustomizeHome,
                            subtitle = "Grid, icon size, app names and dock, with a live preview",
                            value = "${settings.homeColumns} per row",
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

                }
                if (section == SettingsSection.DRAWER) {
                    SettingsGroup("App drawer and categories", "📱") {
                        SettingChoice(
                            "Open the drawer in",
                            listOf(true to "Categories", false to "All Apps"),
                            smart.groupDrawerByCategory,
                            { onSmartChange(smart.copy(groupDrawerByCategory = it)) },
                            subtitle = "You can also switch at the top of the drawer. Categories shows one category at a time: swipe sideways to change.",
                        )
                        SettingDivider()
                        SettingRow(
                            "Customize app drawer",
                            onClick = onOpenCustomizeDrawer,
                            subtitle = "Layout, grid, icon size and app names for the drawer, with a live preview",
                            value = if (settings.drawerLayout == DrawerLayout.LIST) "List" else "${settings.columns} per row",
                        )
                        SettingDivider()
                        SettingRow(
                            "Manage categories",
                            onClick = onOpenCategories,
                            subtitle = "Rename, hide, reorder or delete categories, or move apps between them",
                        )
                    }

                }
                if (section == SettingsSection.DRAWER) {
                    SearchSettingsGroup(smart = smart, onChange = onSmartChange)

                }
                if (section == SettingsSection.BEHAVIOR) {
                    SettingsGroup("Gestures and navigation", "👆") {
                        SettingChoice("Swipe up", gestureOptions, settings.swipeUp, { onChange(settings.copy(swipeUp = it)) })
                        SettingDivider()
                        SettingChoice("Swipe down", gestureOptions, settings.swipeDown, { onChange(settings.copy(swipeDown = it)) })
                        SettingDivider()
                        SettingChoice("Double tap", gestureOptions, settings.doubleTap, { onChange(settings.copy(doubleTap = it)) })
                        SettingDivider()
                        SettingRow(
                            "Default launcher",
                            onClick = onOpenSetup,
                            subtitle = when {
                                status.isDefault -> "My Launcher is your home app"
                                status.currentLabel != null -> "Your home app is ${status.currentLabel}"
                                else -> "No home app chosen yet"
                            },
                        )
                    }
                    SettingNote(
                        "Long press on the home screen always opens the edit menu. Locking the screen is not " +
                            "offered because Android only allows it with an Accessibility Service.",
                    )

                }
                if (section == SettingsSection.DRAWER) {
                    SettingsGroup("App management", "🧰") {
                        SettingInfo(
                            "Select several apps",
                            "Tap the check mark at the top of the app drawer, or long-press an app and choose \"Select several apps\". " +
                                "Then change category, add to favorites, hide, lock, move to a folder or uninstall them together.",
                        )
                        SettingDivider()
                        SettingRow(
                            "App usage timer",
                            onClick = onOpenTimer,
                            subtitle = "Daily time limits for chosen apps, unlocked with a separate parent PIN",
                            value = timerSummary,
                        )
                    }

                }
                if (section == SettingsSection.DRAWER) {
                    SettingsGroup("Recently used apps and suggestions", "🕘") {
                        SettingSwitch(
                            "Remember recently used apps", settings.trackLaunches,
                            { onChange(settings.copy(trackLaunches = it)) },
                            subtitle = "Only apps opened from this launcher, stored on this device. No special permission needed.",
                        )
                        SettingDivider()
                        SettingSwitch(
                            "Show recently used apps", smart.showRecentApps, { onSmartChange(smart.copy(showRecentApps = it)) },
                            subtitle = if (settings.trackLaunches) {
                                "A row of apps you opened lately, at the top of the drawer"
                            } else {
                                "Needs \"Remember recently used apps\" above, otherwise there is nothing to show"
                            },
                        )
                        SettingDivider()
                        SettingSwitch(
                            "Show suggested apps", smart.smartSuggestions, { onSmartChange(smart.copy(smartSuggestions = it)) },
                            subtitle = if (settings.trackLaunches) {
                                "A row of apps you often open at this time of day. Uses only data on this device."
                            } else {
                                "Needs \"Remember recently used apps\" above to have anything to suggest"
                            },
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

                }
                if (section == SettingsSection.PRIVACY) {
                    SettingsGroup("Privacy and hidden apps", "🔒") {
                        SettingRow(
                            "Private apps",
                            onClick = onOpenHidden,
                            subtitle = "Hidden inside this launcher only. They stay installed and visible to Android and other launchers.",
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
                    }

                }
                if (section == SettingsSection.BEHAVIOR) {
                    SettingsGroup("Performance", "⚡") {
                        SettingChoice(
                            "Performance mode",
                            PerformanceMode.entries.map { it to it.label },
                            settings.performance,
                            { onChange(settings.copy(performance = it)) },
                            subtitle = settings.performance.description,
                        )
                    }

                }
                if (section == SettingsSection.MORE) {
                    SettingsGroup("Backup and device", "💾") {
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

                }
                if (section == SettingsSection.MORE) {
                    SettingsGroup("About and developer", "ℹ️") {
                        SettingInfo("App", "My Launcher")
                        SettingDivider()
                        SettingInfo("Developer", "Azi Creation")
                        SettingDivider()
                        SettingInfo("Version", version)
                        SettingDivider()
                        SettingRow(
                            "Version history",
                            onClick = { showHistory = true },
                            subtitle = "What changed in each version",
                        )
                        SettingDivider()
                        SettingRow(
                            "Support Azi Creation",
                            onClick = { showSupport = true },
                            subtitle = "Optional. Details will be added later.",
                            value = "Coming soon",
                        )
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

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Version history") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    VersionHistory.releases.forEach { release ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${release.version} · ${release.title}", style = MaterialTheme.typography.titleSmall)
                            release.points.forEach { point ->
                                Text("• $point", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close") } },
        )
    }

    if (showSupport) {
        AlertDialog(
            onDismissRequest = { showSupport = false },
            title = { Text("Support Azi Creation") },
            text = {
                Text(
                    "My Launcher is free, with no ads. An optional way to say thanks may be added in a later version. " +
                        "There is nothing to pay or scan yet.",
                )
            },
            confirmButton = { TextButton(onClick = { showSupport = false }) { Text("OK") } },
        )
    }
}

/** One card on the Settings dashboard. */
@Composable
private fun SectionCard(section: SettingsSection, summary: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(section.emoji, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    section.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
