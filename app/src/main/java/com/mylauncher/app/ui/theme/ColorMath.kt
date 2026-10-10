package com.mylauncher.app.ui.theme

import kotlin.math.pow

/** Small, pure color helpers on packed ARGB ints (so they are unit-testable without Android). */
object ColorMath {
    const val WHITE: Int = -1 // 0xFFFFFFFF
    const val BLACK: Int = -16777216 // 0xFF000000

    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF

    fun rgb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** Opaque mix of [from] and [to]; t = 0 gives [from], t = 1 gives [to]. */
    fun blend(from: Int, to: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun mix(a: Int, b: Int) = (a + (b - a) * k).toInt()
        return rgb(mix(red(from), red(to)), mix(green(from), green(to)), mix(blue(from), blue(to)))
    }

    /** WCAG relative luminance, 0 (black) to 1 (white). */
    fun luminance(c: Int): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red(c)) + 0.7152 * channel(green(c)) + 0.0722 * channel(blue(c))
    }

    fun contrastRatio(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    /** Black or white, whichever reads better on [background]. */
    fun onColor(background: Int): Int =
        if (contrastRatio(background, WHITE) >= contrastRatio(background, BLACK)) WHITE else BLACK

    /** Accepts "#RRGGBB" or "RRGGBB". Returns null for anything else. */
    fun parseHex(text: String): Int? {
        val t = text.trim().removePrefix("#")
        if (t.length != 6 || !t.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        val v = t.toInt(16)
        return rgb((v shr 16) and 0xFF, (v shr 8) and 0xFF, v and 0xFF)
    }

    fun toHex(c: Int): String = "#%02X%02X%02X".format(red(c), green(c), blue(c))
}

/** Derives a full, readable color set from one accent color. Pure; the Compose mapping lives in Theme.kt. */
object AccentPalette {
    data class Colors(
        val primary: Int, val onPrimary: Int, val primaryContainer: Int, val onPrimaryContainer: Int,
        val secondary: Int, val onSecondary: Int, val secondaryContainer: Int, val onSecondaryContainer: Int,
        val tertiary: Int, val onTertiary: Int, val tertiaryContainer: Int, val onTertiaryContainer: Int,
        val background: Int, val onBackground: Int,
        val surface: Int, val onSurface: Int, val surfaceVariant: Int, val onSurfaceVariant: Int,
        val outline: Int, val outlineVariant: Int,
        val surfaceDim: Int, val surfaceBright: Int,
        val surfaceContainerLowest: Int, val surfaceContainerLow: Int, val surfaceContainer: Int,
        val surfaceContainerHigh: Int, val surfaceContainerHighest: Int,
        val inverseSurface: Int, val inverseOnSurface: Int, val inversePrimary: Int,
    )

    private const val LIGHT_BASE = -1 // white
    private const val DARK_BASE = 0xFF121212.toInt()

    fun derive(accent: Int, dark: Boolean): Colors {
        val base = if (dark) DARK_BASE else LIGHT_BASE
        val primary = if (dark) ColorMath.blend(accent, ColorMath.WHITE, 0.40f) else accent
        val secondary = ColorMath.blend(primary, if (dark) 0xFFB0B0B0.toInt() else 0xFF606060.toInt(), 0.55f)
        val tertiary = ColorMath.blend(primary, 0xFFB5527A.toInt(), 0.55f)

        fun container(c: Int) = if (dark) ColorMath.blend(c, ColorMath.BLACK, 0.55f) else ColorMath.blend(c, ColorMath.WHITE, 0.80f)
        val primaryContainer = container(accent)
        val secondaryContainer = container(secondary)
        val tertiaryContainer = container(tertiary)

        fun tint(t: Float) = ColorMath.blend(base, accent, t)
        val surface = tint(if (dark) 0.05f else 0.02f)
        val onSurface = if (dark) 0xFFE6E1E5.toInt() else 0xFF1C1B1F.toInt()
        val surfaceVariant = tint(if (dark) 0.14f else 0.10f)
        val onSurfaceVariant = if (dark) 0xFFCAC4D0.toInt() else 0xFF49454F.toInt()

        return Colors(
            primary = primary, onPrimary = ColorMath.onColor(primary),
            primaryContainer = primaryContainer, onPrimaryContainer = ColorMath.onColor(primaryContainer),
            secondary = secondary, onSecondary = ColorMath.onColor(secondary),
            secondaryContainer = secondaryContainer, onSecondaryContainer = ColorMath.onColor(secondaryContainer),
            tertiary = tertiary, onTertiary = ColorMath.onColor(tertiary),
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = ColorMath.onColor(tertiaryContainer),
            background = surface, onBackground = onSurface,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = ColorMath.blend(onSurfaceVariant, surface, 0.45f),
            outlineVariant = ColorMath.blend(onSurfaceVariant, surface, 0.75f),
            surfaceDim = tint(if (dark) 0.03f else 0.07f),
            surfaceBright = tint(if (dark) 0.12f else 0.01f),
            surfaceContainerLowest = tint(if (dark) 0.02f else 0.0f),
            surfaceContainerLow = tint(if (dark) 0.06f else 0.03f),
            surfaceContainer = tint(if (dark) 0.09f else 0.05f),
            surfaceContainerHigh = tint(if (dark) 0.12f else 0.07f),
            surfaceContainerHighest = tint(if (dark) 0.15f else 0.09f),
            inverseSurface = if (dark) 0xFFE6E1E5.toInt() else 0xFF313033.toInt(),
            inverseOnSurface = if (dark) 0xFF313033.toInt() else 0xFFF4EFF4.toInt(),
            inversePrimary = if (dark) accent else ColorMath.blend(accent, ColorMath.WHITE, 0.45f),
        )
    }
}

data class AccentPreset(val name: String, val argb: Int)

object ThemePresets {
    val accents: List<AccentPreset> = listOf(
        AccentPreset("Blue", 0xFF3567C9.toInt()),
        AccentPreset("Teal", 0xFF0F7C7C.toInt()),
        AccentPreset("Green", 0xFF2E7D32.toInt()),
        AccentPreset("Orange", 0xFFC25E00.toInt()),
        AccentPreset("Red", 0xFFC62828.toInt()),
        AccentPreset("Pink", 0xFFB02A6B.toInt()),
        AccentPreset("Purple", 0xFF6B4BC2.toInt()),
        AccentPreset("Slate", 0xFF475569.toInt()),
    )
    val default: Int get() = accents.first().argb
}
