package com.mylauncher.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mylauncher.app.data.model.SearchBarStyle
import com.mylauncher.app.data.model.SurfaceStyle
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.model.ThemeSettings

/** Shape used by the home and drawer search fields (chosen in Settings > Themes). */
val LocalSearchBarShape = staticCompositionLocalOf<Shape> { CircleShape }

fun dynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Whether wallpaper colors should be used for this combination of mode and settings. */
internal fun wantsDynamicColor(mode: ThemeMode, settings: ThemeSettings): Boolean =
    dynamicColorSupported() && (mode == ThemeMode.DYNAMIC || (mode != ThemeMode.CUSTOM && settings.dynamicColor))

@Composable
fun MyLauncherTheme(
    mode: ThemeMode,
    /** The home screen sits on the wallpaper, so system bar icons stay light there. */
    homeVisible: Boolean,
    themeSettings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM, ThemeMode.DYNAMIC, ThemeMode.CUSTOM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    // Built once per input change instead of on every recomposition.
    val colorScheme: ColorScheme = remember(mode, themeSettings, dark, context) {
        val baseScheme: ColorScheme = when {
            wantsDynamicColor(mode, themeSettings) ->
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            mode == ThemeMode.CUSTOM || themeSettings.accentColor != ThemeSettings.NO_ACCENT -> {
                val accent = if (themeSettings.accentColor != ThemeSettings.NO_ACCENT) themeSettings.accentColor else ThemePresets.default
                accentScheme(accent, dark)
            }
            dark -> darkColorScheme()
            else -> lightColorScheme()
        }
        applySurfaceStyle(baseScheme, themeSettings.surfaceStyle, dark)
    }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val lightIcons = !dark && !homeVisible
        controller.isAppearanceLightStatusBars = lightIcons
        controller.isAppearanceLightNavigationBars = lightIcons
    }

    val baseDensity = LocalDensity.current
    val density = remember(baseDensity, themeSettings.textScale) {
        Density(baseDensity.density, baseDensity.fontScale * themeSettings.textScale.factor)
    }
    val shapes = remember(themeSettings.cornerRadius) { shapesFor(themeSettings.cornerRadius) }
    val searchShape: Shape = when (themeSettings.searchBarStyle) {
        SearchBarStyle.PILL -> CircleShape
        SearchBarStyle.ROUNDED -> RoundedCornerShape(16.dp)
        SearchBarStyle.SQUARE -> RoundedCornerShape(4.dp)
    }

    CompositionLocalProvider(LocalDensity provides density, LocalSearchBarShape provides searchShape) {
        MaterialTheme(colorScheme = colorScheme, shapes = shapes, content = content)
    }
}

/** radius = 16 gives the standard Material 3 shapes (4, 8, 12, 16, 28); other values scale them. */
internal fun shapesFor(radiusDp: Int): Shapes {
    val k = radiusDp.coerceIn(ThemeSettings.MIN_RADIUS, ThemeSettings.MAX_RADIUS) / ThemeSettings.DEFAULT_RADIUS.toFloat()
    fun shape(base: Float) = RoundedCornerShape((base * k).dp)
    return Shapes(
        extraSmall = shape(4f),
        small = shape(8f),
        medium = shape(12f),
        large = shape(16f),
        extraLarge = shape(28f),
    )
}

private fun accentScheme(accent: Int, dark: Boolean): ColorScheme {
    val c = AccentPalette.derive(accent, dark)
    // Start from the stock scheme (it supplies the error colors) and replace everything accent-related.
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(c.primary), onPrimary = Color(c.onPrimary),
        primaryContainer = Color(c.primaryContainer), onPrimaryContainer = Color(c.onPrimaryContainer),
        inversePrimary = Color(c.inversePrimary),
        secondary = Color(c.secondary), onSecondary = Color(c.onSecondary),
        secondaryContainer = Color(c.secondaryContainer), onSecondaryContainer = Color(c.onSecondaryContainer),
        tertiary = Color(c.tertiary), onTertiary = Color(c.onTertiary),
        tertiaryContainer = Color(c.tertiaryContainer), onTertiaryContainer = Color(c.onTertiaryContainer),
        background = Color(c.background), onBackground = Color(c.onBackground),
        surface = Color(c.surface), onSurface = Color(c.onSurface),
        surfaceVariant = Color(c.surfaceVariant), onSurfaceVariant = Color(c.onSurfaceVariant),
        surfaceTint = Color(c.primary),
        inverseSurface = Color(c.inverseSurface), inverseOnSurface = Color(c.inverseOnSurface),
        outline = Color(c.outline), outlineVariant = Color(c.outlineVariant),
        surfaceBright = Color(c.surfaceBright), surfaceDim = Color(c.surfaceDim),
        surfaceContainerLowest = Color(c.surfaceContainerLowest), surfaceContainerLow = Color(c.surfaceContainerLow),
        surfaceContainer = Color(c.surfaceContainer), surfaceContainerHigh = Color(c.surfaceContainerHigh),
        surfaceContainerHighest = Color(c.surfaceContainerHighest),
    )
}

private fun applySurfaceStyle(scheme: ColorScheme, style: SurfaceStyle, dark: Boolean): ColorScheme = when {
    style == SurfaceStyle.AMOLED && dark -> scheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF080808),
        surfaceContainer = Color(0xFF101010),
        surfaceContainerHigh = Color(0xFF181818),
        surfaceContainerHighest = Color(0xFF202020),
    )
    style == SurfaceStyle.TINTED -> {
        fun tinted(c: Color) = Color(ColorMath.blend(c.toArgb(), scheme.primary.toArgb(), 0.10f))
        scheme.copy(background = tinted(scheme.background), surface = tinted(scheme.surface))
    }
    else -> scheme
}
