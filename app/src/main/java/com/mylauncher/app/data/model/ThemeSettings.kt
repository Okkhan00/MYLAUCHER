package com.mylauncher.app.data.model

/** How the launcher's own surfaces (drawer, settings, dialogs) are colored. */
enum class SurfaceStyle(val label: String) { DEFAULT("Default"), TINTED("Tinted"), AMOLED("Pure black (dark)") }

enum class TextScale(val label: String, val factor: Float) {
    SMALL("Small", 0.9f), NORMAL("Normal", 1f), LARGE("Large", 1.15f), XLARGE("Extra large", 1.3f)
}

enum class SearchBarStyle(val label: String) { PILL("Pill"), ROUNDED("Rounded"), SQUARE("Square") }

/**
 * Visual customization on top of [ThemeMode]. Defaults reproduce the earlier look exactly:
 * wallpaper colors on Android 12+, the standard Material shapes and a 96% opaque drawer.
 */
data class ThemeSettings(
    /** Use wallpaper colors for the SYSTEM/LIGHT/DARK themes where Android supports it (12+). */
    val dynamicColor: Boolean = true,
    /** ARGB accent used by the CUSTOM theme (and by other themes when wallpaper colors are unavailable). 0 = none. */
    val accentColor: Int = NO_ACCENT,
    val surfaceStyle: SurfaceStyle = SurfaceStyle.DEFAULT,
    val textScale: TextScale = TextScale.NORMAL,
    val searchBarStyle: SearchBarStyle = SearchBarStyle.PILL,
    /** Base corner radius in dp for tiles, cards and dialogs (16 reproduces the original shapes). */
    val cornerRadius: Int = DEFAULT_RADIUS,
    /** Opacity of the app drawer background, in percent. */
    val drawerOpacity: Int = DEFAULT_DRAWER_OPACITY,
) {
    companion object {
        const val NO_ACCENT = 0
        const val MIN_RADIUS = 0
        const val MAX_RADIUS = 28
        const val DEFAULT_RADIUS = 16
        const val MIN_DRAWER_OPACITY = 60
        const val MAX_DRAWER_OPACITY = 100
        const val DEFAULT_DRAWER_OPACITY = 96
    }
}

enum class OverlayStyle(val label: String) { NONE("None"), DARK("Dark"), LIGHT("Light") }

enum class BlurLevel(val label: String, val radiusPx: Int) {
    OFF("Off", 0), LOW("Low", 20), MEDIUM("Medium", 50), HIGH("High", 90)
}

/** Wallpaper presentation. The wallpaper itself is chosen with Android's own picker. */
data class WallpaperSettings(
    val overlay: OverlayStyle = OverlayStyle.DARK,
    /** Strength of the overlay, in percent. 22 reproduces the original dimming. */
    val overlayPercent: Int = DEFAULT_OVERLAY_PERCENT,
    val blur: BlurLevel = BlurLevel.OFF,
    /** Also show the overlay behind the app drawer when the drawer is translucent. */
    val drawerOverlay: Boolean = false,
) {
    companion object {
        const val MIN_OVERLAY_PERCENT = 0
        const val MAX_OVERLAY_PERCENT = 70
        const val DEFAULT_OVERLAY_PERCENT = 22
    }
}
