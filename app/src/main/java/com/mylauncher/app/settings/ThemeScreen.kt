package com.mylauncher.app.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SearchBarStyle
import com.mylauncher.app.data.model.SurfaceStyle
import com.mylauncher.app.data.model.TextScale
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.ui.components.ChoiceRow
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SectionTitle
import com.mylauncher.app.ui.components.SwitchRow
import com.mylauncher.app.ui.theme.ThemePresets
import com.mylauncher.app.ui.theme.dynamicColorSupported

/** Colors, shapes, text and drawer appearance. Everything previews live because the whole app re-themes. */
@Composable
fun ThemeScreen(
    settings: LauncherSettings,
    theme: ThemeSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onThemeChange: (ThemeSettings) -> Unit,
    onBack: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Themes and colors", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Preview", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Cards, dialogs and tiles use this corner radius and text size.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SectionTitle("Theme")
                ChoiceRow(
                    "Mode",
                    listOf(
                        ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark",
                        ThemeMode.DYNAMIC to "Dynamic", ThemeMode.CUSTOM to "Custom",
                    ),
                    settings.theme,
                ) { onSettingsChange(settings.copy(theme = it)) }
                Text(
                    "Dynamic uses wallpaper colors. Custom uses the accent color below. " +
                        "Both follow the system light/dark setting.",
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SwitchRow(
                    "Use wallpaper colors",
                    theme.dynamicColor && dynamicColorSupported(),
                    { if (dynamicColorSupported()) onThemeChange(theme.copy(dynamicColor = it)) },
                    subtitle = if (dynamicColorSupported()) {
                        "For System, Light and Dark themes (Material You)"
                    } else {
                        "Needs Android 12 or newer. A built-in color set is used instead."
                    },
                )

                SectionTitle("Accent color")
                AccentChips(theme, onThemeChange)
                Text(
                    "Used by the Custom theme, and by other themes when wallpaper colors are off or unavailable.",
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceRow(
                    "Surfaces",
                    SurfaceStyle.entries.map { it to it.label },
                    theme.surfaceStyle,
                ) { onThemeChange(theme.copy(surfaceStyle = it)) }

                SectionTitle("Shape and text")
                ChoiceRow("Text size", TextScale.entries.map { it to it.label }, theme.textScale) {
                    onThemeChange(theme.copy(textScale = it))
                }
                SliderRow(
                    title = "Corner radius: ${theme.cornerRadius} dp",
                    value = theme.cornerRadius.toFloat(),
                    range = ThemeSettings.MIN_RADIUS.toFloat()..ThemeSettings.MAX_RADIUS.toFloat(),
                    onChange = { onThemeChange(theme.copy(cornerRadius = it.toInt())) },
                )
                ChoiceRow("Search bar", SearchBarStyle.entries.map { it to it.label }, theme.searchBarStyle) {
                    onThemeChange(theme.copy(searchBarStyle = it))
                }

                SectionTitle("App drawer")
                SliderRow(
                    title = "Drawer opacity: ${theme.drawerOpacity}%",
                    value = theme.drawerOpacity.toFloat(),
                    range = ThemeSettings.MIN_DRAWER_OPACITY.toFloat()..ThemeSettings.MAX_DRAWER_OPACITY.toFloat(),
                    onChange = { onThemeChange(theme.copy(drawerOpacity = it.toInt())) },
                )
                SwitchRow(
                    "Reduce animations", !settings.animations, { onSettingsChange(settings.copy(animations = !it)) },
                    subtitle = "Same as Settings > Animations (off). New effects always follow it.",
                )
                Text(
                    "Icon size, clock and grid options are in Settings > Appearance.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccentChips(theme: ThemeSettings, onThemeChange: (ThemeSettings) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Names are shown with every swatch so color is never the only signal.
        FilterChip(
            selected = theme.accentColor == ThemeSettings.NO_ACCENT,
            onClick = { onThemeChange(theme.copy(accentColor = ThemeSettings.NO_ACCENT)) },
            label = { Text("None") },
        )
        ThemePresets.accents.forEach { preset ->
            FilterChip(
                selected = theme.accentColor == preset.argb,
                onClick = { onThemeChange(theme.copy(accentColor = preset.argb)) },
                label = { Text(preset.name) },
                leadingIcon = {
                    Box(Modifier.size(14.dp).background(Color(preset.argb), CircleShape))
                },
                modifier = Modifier.semantics { contentDescription = "${preset.name} accent" },
            )
        }
    }
}

@Composable
internal fun SliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}
