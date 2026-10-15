package com.mylauncher.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SettingChoice
import com.mylauncher.app.ui.components.SettingDivider
import com.mylauncher.app.ui.components.SettingRow
import com.mylauncher.app.ui.components.SettingSwitch
import com.mylauncher.app.ui.components.SettingsGroup

private val iconSizeOptions = listOf(
    IconSize.SMALL to "Small",
    IconSize.MEDIUM to "Medium",
    IconSize.LARGE to "Large",
)

private fun sizeHint(size: IconSize) = "Now ${size.sizeDp} dp"

/**
 * Home screen customization: its own grid, icon size, names and dock, with a live preview.
 * Everything here only changes the home screen; the app drawer has its own page.
 */
@Composable
fun CustomizeHomeScreen(
    settings: LauncherSettings,
    onChange: (LauncherSettings) -> Unit,
    onOpenThemes: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Customize home screen", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                LayoutPreview(
                    columns = settings.homeColumns,
                    iconSize = settings.homeIconSize,
                    showLabels = settings.homeShowLabels,
                    dockCount = settings.dockCount,
                    caption = "Home screen preview",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "Changes apply to your home screen straight away and only affect the home screen.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsGroup("Grid and icons", "🏠") {
                    SettingChoice(
                        "Apps per row",
                        listOf(4 to "4", 5 to "5", 6 to "6"),
                        settings.homeColumns,
                        { onChange(settings.copy(homeColumns = it)) },
                        subtitle = "Your icons keep their order and flow into the new rows.",
                    )
                    SettingDivider()
                    SettingChoice(
                        "Icon size",
                        iconSizeOptions,
                        settings.homeIconSize,
                        { onChange(settings.copy(homeIconSize = it)) },
                        subtitle = sizeHint(settings.homeIconSize),
                    )
                    SettingDivider()
                    SettingSwitch(
                        "Show app names",
                        settings.homeShowLabels,
                        { onChange(settings.copy(homeShowLabels = it)) },
                        subtitle = "Names under home screen icons. The app drawer is not affected.",
                    )
                }

                SettingsGroup("Dock", "📌") {
                    SettingChoice(
                        "Dock size",
                        listOf(0 to "Off", 3 to "3 apps", 4 to "4 apps", 5 to "5 apps"),
                        settings.dockCount,
                        { onChange(settings.copy(dockCount = it)) },
                        subtitle = "A row of apps at the bottom. Long-press an app on the home screen or in the drawer and choose Add to dock. Lowering the size sends extra apps back to the home screen.",
                    )
                }

                SettingsGroup("Colors", "🎨") {
                    SettingRow(
                        "Theme and accent color",
                        onClick = onOpenThemes,
                        subtitle = "The preview above uses your current theme",
                    )
                }

                SettingsGroup("Reset", "↺") {
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
                    ) { Text("Reset home screen layout") }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset home screen layout?") },
            text = {
                Text(
                    "Apps per row, icon size, app names and dock size go back to their defaults. " +
                        "Your home apps, folders and their order are kept, and dock apps return to the home screen. " +
                        "The app drawer is not changed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    val d = LauncherSettings()
                    onChange(
                        settings.copy(
                            homeColumns = d.homeColumns,
                            homeIconSize = d.homeIconSize,
                            homeShowLabels = d.homeShowLabels,
                            dockCount = d.dockCount,
                        ),
                    )
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

/** App drawer customization: its own grid, icon size, names and layout, with a live preview. */
@Composable
fun CustomizeDrawerScreen(
    settings: LauncherSettings,
    onChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit,
) {
    var confirmReset by remember { mutableStateOf(false) }
    val list = settings.drawerLayout == DrawerLayout.LIST

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Customize app drawer", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                LayoutPreview(
                    columns = settings.columns,
                    iconSize = settings.iconSize,
                    showLabels = settings.showLabels,
                    list = list,
                    caption = "App drawer preview",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Text(
                    "Changes apply to the app drawer (All Apps and each category) straight away and do not change the home screen.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsGroup("Layout and icons", "📱") {
                    SettingChoice(
                        "Layout",
                        listOf(DrawerLayout.GRID to "Grid", DrawerLayout.LIST to "List"),
                        settings.drawerLayout,
                        { onChange(settings.copy(drawerLayout = it)) },
                        subtitle = "For All Apps and inside a category",
                    )
                    SettingDivider()
                    SettingChoice(
                        "Apps per row",
                        listOf(4 to "4", 5 to "5", 6 to "6"),
                        settings.columns,
                        { onChange(settings.copy(columns = it)) },
                        subtitle = if (list) "Used when the layout is Grid" else null,
                    )
                    SettingDivider()
                    SettingChoice(
                        "Icon size",
                        iconSizeOptions,
                        settings.iconSize,
                        { onChange(settings.copy(iconSize = it)) },
                        subtitle = sizeHint(settings.iconSize),
                    )
                    SettingDivider()
                    SettingSwitch(
                        "Show app names",
                        settings.showLabels,
                        { onChange(settings.copy(showLabels = it)) },
                        subtitle = "Names in the app drawer. The home screen is not affected.",
                    )
                }

                SettingsGroup("Reset", "↺") {
                    OutlinedButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
                    ) { Text("Reset app drawer layout") }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset app drawer layout?") },
            text = {
                Text(
                    "Layout, apps per row, icon size and app names go back to their defaults. " +
                        "Categories, favorites, hidden apps and the home screen are not changed.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    val d = LauncherSettings()
                    onChange(
                        settings.copy(
                            columns = d.columns,
                            iconSize = d.iconSize,
                            showLabels = d.showLabels,
                            drawerLayout = d.drawerLayout,
                        ),
                    )
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}
