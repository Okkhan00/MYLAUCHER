package com.mylauncher.app.ui.theme

import com.mylauncher.app.data.model.CustomTheme
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SurfaceStyle
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.model.ThemeSettings

/**
 * A ready-made theme. It is only a named combination of the options that already exist (mode, accent
 * color, surface style), so applying one needs no extra engine: the whole launcher re-themes through the
 * same Material 3 path as before. These definitions are constants and are never edited by the user.
 */
data class BuiltInTheme(
    val id: String,
    val name: String,
    val description: String,
    val mode: ThemeMode,
    /** Only meaningful for SYSTEM/LIGHT/DARK with no accent: use wallpaper colors on Android 12+. */
    val dynamicColor: Boolean,
    val accent: Int,
    val surface: SurfaceStyle,
) {
    fun matches(currentMode: ThemeMode, theme: ThemeSettings): Boolean =
        mode == currentMode &&
            accent == theme.accentColor &&
            surface == theme.surfaceStyle &&
            (mode == ThemeMode.CUSTOM || mode == ThemeMode.DYNAMIC || dynamicColor == theme.dynamicColor)
}

object BuiltInThemes {
    const val CUSTOM_ID = "custom"
    const val DEFAULT_ID = "default"

    val all: List<BuiltInTheme> = listOf(
        BuiltInTheme(DEFAULT_ID, "Default", "Follows your phone, with wallpaper colors on Android 12+",
            ThemeMode.SYSTEM, true, ThemeSettings.NO_ACCENT, SurfaceStyle.DEFAULT),
        BuiltInTheme("midnight", "Midnight Dark", "Deep blue-violet, always dark",
            ThemeMode.DARK, false, 0xFF5C6BC0.toInt(), SurfaceStyle.DEFAULT),
        BuiltInTheme("clean_light", "Clean Light", "Bright and neutral, always light",
            ThemeMode.LIGHT, false, 0xFF1A5FD0.toInt(), SurfaceStyle.DEFAULT),
        BuiltInTheme("ocean", "Ocean Blue", "Calm blue, follows light or dark",
            ThemeMode.CUSTOM, false, 0xFF0277BD.toInt(), SurfaceStyle.TINTED),
        BuiltInTheme("forest", "Forest Green", "Natural green, follows light or dark",
            ThemeMode.CUSTOM, false, 0xFF2E7D32.toInt(), SurfaceStyle.TINTED),
        BuiltInTheme("royal", "Royal Purple", "Rich purple, follows light or dark",
            ThemeMode.CUSTOM, false, 0xFF6A1B9A.toInt(), SurfaceStyle.TINTED),
        BuiltInTheme("amber", "Warm Amber", "Warm orange, follows light or dark",
            ThemeMode.CUSTOM, false, 0xFFB45309.toInt(), SurfaceStyle.TINTED),
        BuiltInTheme("grey", "Minimal Grey", "Quiet slate grey, follows light or dark",
            ThemeMode.CUSTOM, false, 0xFF546E7A.toInt(), SurfaceStyle.DEFAULT),
        BuiltInTheme("amoled", "AMOLED Black", "Pure black, saves power on OLED screens",
            ThemeMode.DARK, false, 0xFF2979FF.toInt(), SurfaceStyle.AMOLED),
    )

    fun byId(id: String): BuiltInTheme? = all.firstOrNull { it.id == id }

    /** The built-in theme the current settings equal, or [CUSTOM_ID] when they are the user's own mix. */
    fun selectedId(mode: ThemeMode, theme: ThemeSettings): String =
        all.firstOrNull { it.matches(mode, theme) }?.id ?: CUSTOM_ID

    /** Display name of the current look, for the Settings row. */
    fun selectedName(mode: ThemeMode, theme: ThemeSettings): String =
        byId(selectedId(mode, theme))?.name ?: "My Custom Theme"

    /** Applies a built-in theme. Text size, corners, search bar, drawer opacity and the custom slot are kept. */
    fun apply(builtIn: BuiltInTheme, settings: LauncherSettings, theme: ThemeSettings): Pair<LauncherSettings, ThemeSettings> =
        settings.copy(theme = builtIn.mode) to theme.copy(
            dynamicColor = builtIn.dynamicColor,
            accentColor = builtIn.accent,
            surfaceStyle = builtIn.surface,
        )

    /** Applies the user's custom theme (and remembers it as the custom slot). */
    fun applyCustom(custom: CustomTheme, settings: LauncherSettings, theme: ThemeSettings): Pair<LauncherSettings, ThemeSettings> =
        settings.copy(theme = custom.mode) to theme.copy(
            dynamicColor = false,
            accentColor = custom.accentColor,
            surfaceStyle = custom.surfaceStyle,
            custom = custom,
        )

    /** The look that is on screen now, as an editable custom theme (the starting point for editing). */
    fun currentAsCustom(mode: ThemeMode, theme: ThemeSettings): CustomTheme {
        val editableMode = when (mode) {
            ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.DYNAMIC, ThemeMode.CUSTOM -> mode
            ThemeMode.SYSTEM -> ThemeMode.CUSTOM
        }
        val accent = if (theme.accentColor == ThemeSettings.NO_ACCENT) theme.custom.accentColor else theme.accentColor
        return CustomTheme(editableMode, accent, theme.surfaceStyle)
    }

    /** Colors for the small preview in the gallery. Mirrors what the real theme produces for the same input. */
    data class Preview(val background: Int, val card: Int, val primary: Int, val onPrimary: Int, val text: Int, val mutedText: Int)

    private const val PREVIEW_FALLBACK_ACCENT = 0xFF6750A4.toInt()

    fun preview(mode: ThemeMode, accent: Int, surface: SurfaceStyle, systemDark: Boolean): Preview {
        val dark = when (mode) {
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
            else -> systemDark
        }
        val c = AccentPalette.derive(if (accent == ThemeSettings.NO_ACCENT) PREVIEW_FALLBACK_ACCENT else accent, dark)
        var background = c.surface
        var card = c.surfaceContainerHigh
        if (surface == SurfaceStyle.AMOLED && dark) {
            background = ColorMath.BLACK
            card = 0xFF181818.toInt()
        } else if (surface == SurfaceStyle.TINTED) {
            background = ColorMath.blend(background, c.primary, 0.10f)
        }
        return Preview(background, card, c.primary, c.onPrimary, c.onSurface, c.onSurfaceVariant)
    }
}

/** Saves the custom slot as one short string: MODE,accent,STYLE. Anything unreadable falls back to defaults. */
object CustomThemeCodec {
    fun encode(c: CustomTheme): String = "${c.mode.name},${c.accentColor},${c.surfaceStyle.name}"

    fun decode(raw: String?): CustomTheme {
        val d = CustomTheme()
        if (raw.isNullOrBlank()) return d
        val p = raw.split(',')
        if (p.size != 3) return d
        val mode = ThemeMode.entries.firstOrNull { it.name == p[0] }
            ?.takeIf { it != ThemeMode.SYSTEM } ?: d.mode
        val accent = p[1].toIntOrNull()?.takeIf { it != ThemeSettings.NO_ACCENT } ?: d.accentColor
        val style = SurfaceStyle.entries.firstOrNull { it.name == p[2] } ?: d.surfaceStyle
        return CustomTheme(mode, accent, style)
    }
}
