package com.mylauncher.app.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.CustomTheme
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SearchBarStyle
import com.mylauncher.app.data.model.SurfaceStyle
import com.mylauncher.app.data.model.TextScale
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SettingChoice
import com.mylauncher.app.ui.components.SettingDivider
import com.mylauncher.app.ui.components.SettingNote
import com.mylauncher.app.ui.components.SettingSwitch
import com.mylauncher.app.ui.components.SettingsGroup
import com.mylauncher.app.ui.theme.BuiltInThemes
import com.mylauncher.app.ui.theme.ThemePresets
import com.mylauncher.app.ui.theme.dynamicColorSupported

/**
 * Theme gallery plus a small custom-theme editor. Choosing a theme applies it immediately: the whole
 * launcher re-themes through the normal Material 3 path, so what the card shows is what you get.
 * Themes only change screens that My Launcher draws, never the rest of Android.
 */
@Composable
fun ThemeScreen(
    settings: LauncherSettings,
    theme: ThemeSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onThemeChange: (ThemeSettings) -> Unit,
    onApplyTheme: (LauncherSettings, ThemeSettings) -> Unit,
    onBack: () -> Unit,
) {
    val selectedId = BuiltInThemes.selectedId(settings.theme, theme)
    val systemDark = isSystemInDarkTheme()
    // What the custom editor shows: the look on screen when it is already custom, otherwise the saved slot.
    val editing: CustomTheme = if (selectedId == BuiltInThemes.CUSTOM_ID) {
        BuiltInThemes.currentAsCustom(settings.theme, theme)
    } else {
        theme.custom
    }
    fun applyCustom(next: CustomTheme) {
        val (s, t) = BuiltInThemes.applyCustom(next, settings, theme)
        onApplyTheme(s, t)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Themes", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Text(
                    "Choose a look. It applies straight away to My Launcher's own screens, not to the rest of Android.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Built-in themes plus "My Custom Theme" as the last card, two per row.
                val cards: List<ThemeCardData> = BuiltInThemes.all.map { t ->
                    ThemeCardData(
                        id = t.id,
                        name = t.name,
                        caption = if (t.mode == ThemeMode.SYSTEM && t.accent == ThemeSettings.NO_ACCENT) "Wallpaper colors on Android 12+" else t.description,
                        preview = BuiltInThemes.preview(t.mode, t.accent, t.surface, systemDark),
                        onSelect = {
                            val (s, th) = BuiltInThemes.apply(t, settings, theme)
                            onApplyTheme(s, th)
                        },
                    )
                } + ThemeCardData(
                    id = BuiltInThemes.CUSTOM_ID,
                    name = "My Custom Theme",
                    caption = "Your own accent, mode and surfaces",
                    preview = BuiltInThemes.preview(
                        if (selectedId == BuiltInThemes.CUSTOM_ID) settings.theme else theme.custom.mode,
                        editing.accentColor, editing.surfaceStyle, systemDark,
                    ),
                    onSelect = { applyCustom(theme.custom) },
                )
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    cards.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { c ->
                                ThemeCard(c, selected = c.id == selectedId, modifier = Modifier.weight(1f))
                            }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }

                SettingsGroup("My Custom Theme", "🎨") {
                    SettingChoice(
                        "Mode",
                        buildList {
                            add(ThemeMode.CUSTOM to "Follow system")
                            add(ThemeMode.LIGHT to "Light")
                            add(ThemeMode.DARK to "Dark")
                            if (dynamicColorSupported()) add(ThemeMode.DYNAMIC to "Wallpaper colors")
                        },
                        editing.mode,
                        { applyCustom(editing.copy(mode = it)) },
                        subtitle = "Changing anything here switches to your own theme. Built-in themes stay as they are.",
                    )
                    SettingDivider()
                    AccentSwatches(editing.accentColor, enabled = editing.mode != ThemeMode.DYNAMIC) {
                        applyCustom(editing.copy(accentColor = it))
                    }
                    SettingDivider()
                    SettingChoice(
                        "Surfaces",
                        SurfaceStyle.entries.map { it to it.label },
                        editing.surfaceStyle,
                        { applyCustom(editing.copy(surfaceStyle = it)) },
                    )
                    SettingDivider()
                    Box(Modifier.fillMaxWidth().padding(16.dp)) {
                        OutlinedButton(
                            onClick = {
                                val default = BuiltInThemes.byId(BuiltInThemes.DEFAULT_ID)!!
                                val (s, t) = BuiltInThemes.apply(default, settings, theme)
                                onApplyTheme(s, t)
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Go back to the Default theme") }
                    }
                }
                SettingNote(
                    "Your custom choices are saved separately and are kept when you try a built-in theme. " +
                        "Wallpaper colors need Android 12 or newer.",
                )

                SettingsGroup("Shape and text", "🔤") {
                    SettingChoice("Text size", TextScale.entries.map { it to it.label }, theme.textScale, {
                        onThemeChange(theme.copy(textScale = it))
                    })
                    SettingDivider()
                    SliderRow(
                        title = "Corner radius: ${theme.cornerRadius} dp",
                        value = theme.cornerRadius.toFloat(),
                        range = ThemeSettings.MIN_RADIUS.toFloat()..ThemeSettings.MAX_RADIUS.toFloat(),
                        onChange = { onThemeChange(theme.copy(cornerRadius = it.toInt())) },
                    )
                    SettingDivider()
                    SettingChoice("Search bar shape", SearchBarStyle.entries.map { it to it.label }, theme.searchBarStyle, {
                        onThemeChange(theme.copy(searchBarStyle = it))
                    })
                }

                SettingsGroup("App drawer", "📱") {
                    SliderRow(
                        title = "Drawer opacity: ${theme.drawerOpacity}%",
                        value = theme.drawerOpacity.toFloat(),
                        range = ThemeSettings.MIN_DRAWER_OPACITY.toFloat()..ThemeSettings.MAX_DRAWER_OPACITY.toFloat(),
                        onChange = { onThemeChange(theme.copy(drawerOpacity = it.toInt())) },
                    )
                    SettingDivider()
                    SettingSwitch(
                        "Reduce animations", !settings.animations, { onSettingsChange(settings.copy(animations = !it)) },
                        subtitle = "Same as Settings > Appearance > Animations (off)",
                    )
                }
                SettingNote("Icon size, clock and grid options are in Settings > Home screen.")
            }
        }
    }
}

private class ThemeCardData(
    val id: String,
    val name: String,
    val caption: String,
    val preview: BuiltInThemes.Preview,
    val onSelect: () -> Unit,
)

@Composable
private fun ThemeCard(data: ThemeCardData, selected: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) primary else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = data.onSelect)
            .semantics { contentDescription = data.name + if (selected) ", selected" else "" },
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniPreview(data.preview)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    data.name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) Text("✓ Selected", style = MaterialTheme.typography.labelSmall, color = primary)
            }
            Text(
                data.caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A tiny static picture of a theme: background, search bar, cards and a button, in that theme's colors. */
@Composable
private fun MiniPreview(p: BuiltInThemes.Preview) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(shape)
            .background(Color(p.background))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.8f).height(14.dp).clip(CircleShape).background(Color(p.card)))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(p.primary)))
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(p.card)))
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(Color(p.card)))
            Text("Aa", color = Color(p.text), style = MaterialTheme.typography.labelMedium)
        }
        Box(Modifier.size(width = 56.dp, height = 12.dp).clip(CircleShape).background(Color(p.primary)))
    }
}

@Composable
private fun AccentSwatches(selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Accent color", style = MaterialTheme.typography.bodyLarge)
        Text(
            if (enabled) "Used for buttons, selected items and highlights" else "Not used while Wallpaper colors is on",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemePresets.accents.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { preset ->
                        val isSelected = enabled && preset.argb == selected
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 64.dp)
                                .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton) { onSelect(preset.argb) }
                                .semantics { contentDescription = "${preset.name} accent" + if (isSelected) ", selected" else "" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.argb).copy(alpha = if (enabled) 1f else 0.4f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Text("✓", color = Color(com.mylauncher.app.ui.theme.ColorMath.onColor(preset.argb)))
                                }
                            }
                            // The name is always shown so color is never the only signal.
                            Text(preset.name, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
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
