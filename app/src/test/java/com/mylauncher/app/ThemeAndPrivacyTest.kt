package com.mylauncher.app

import com.mylauncher.app.data.model.BlurLevel
import com.mylauncher.app.data.model.OverlayStyle
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.data.model.WallpaperSettings
import com.mylauncher.app.privacy.AppLockPolicy
import com.mylauncher.app.ui.theme.AccentPalette
import com.mylauncher.app.ui.theme.ColorMath
import com.mylauncher.app.ui.theme.ThemePresets
import com.mylauncher.app.wallpaper.WallpaperMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeAndPrivacyTest {
    @Test fun defaultsReproducePhaseTwoLook() {
        val t = ThemeSettings()
        assertTrue(t.dynamicColor)
        assertEquals(16, t.cornerRadius)
        assertEquals(96, t.drawerOpacity)
        assertEquals(ThemeSettings.NO_ACCENT, t.accentColor)
        val w = WallpaperSettings()
        assertEquals(OverlayStyle.DARK, w.overlay)
        assertEquals(0.22f, WallpaperMath.alpha(w))
        assertEquals(BlurLevel.OFF, w.blur)
    }

    @Test fun overlayAlphaAndBlurRules() {
        assertEquals(0f, WallpaperMath.alpha(WallpaperSettings(overlay = OverlayStyle.NONE, overlayPercent = 50)))
        assertEquals(0.7f, WallpaperMath.alpha(WallpaperSettings(overlayPercent = 500)))
        assertEquals(0f, WallpaperMath.alpha(WallpaperSettings(overlayPercent = -5)))
        assertEquals(BlurLevel.HIGH.radiusPx, WallpaperMath.blurRadiusPx(WallpaperSettings(blur = BlurLevel.HIGH), true))
        assertEquals(0, WallpaperMath.blurRadiusPx(WallpaperSettings(blur = BlurLevel.HIGH), false))
    }

    @Test fun colorBlendAndHex() {
        assertEquals(ColorMath.WHITE, ColorMath.blend(ColorMath.BLACK, ColorMath.WHITE, 1f))
        assertEquals(ColorMath.BLACK, ColorMath.blend(ColorMath.BLACK, ColorMath.WHITE, 0f))
        assertEquals("#3567C9", ColorMath.toHex(ColorMath.parseHex("#3567c9")!!))
        assertNull(ColorMath.parseHex("blue"))
        assertNull(ColorMath.parseHex("#12345"))
    }

    @Test fun onColorPicksReadableText() {
        assertEquals(ColorMath.WHITE, ColorMath.onColor(ColorMath.BLACK))
        assertEquals(ColorMath.BLACK, ColorMath.onColor(ColorMath.WHITE))
    }

    @Test fun everyPresetGivesReadableTextInLightAndDark() {
        ThemePresets.accents.forEach { preset ->
            listOf(false, true).forEach { dark ->
                val c = AccentPalette.derive(preset.argb, dark)
                assertTrue("${preset.name} dark=$dark onPrimary", ColorMath.contrastRatio(c.primary, c.onPrimary) >= 4.5)
                assertTrue("${preset.name} dark=$dark onSurface", ColorMath.contrastRatio(c.surface, c.onSurface) >= 7.0)
                assertTrue("${preset.name} dark=$dark container", ColorMath.contrastRatio(c.primaryContainer, c.onPrimaryContainer) >= 4.5)
            }
        }
    }

    @Test fun appLockOnlyAppliesWhenAPinExists() {
        val withPin = SecuritySettings(pinHash = "h", pinSalt = "s")
        val locked = setOf("com.bank")
        assertTrue(AppLockPolicy.requiresAuth("com.bank", withPin, locked))
        assertFalse(AppLockPolicy.requiresAuth("com.other", withPin, locked))
        assertFalse(AppLockPolicy.requiresAuth("com.bank", SecuritySettings(), locked))
        assertEquals(locked, AppLockPolicy.inactiveLocks(SecuritySettings(), locked))
        assertTrue(AppLockPolicy.inactiveLocks(withPin, locked).isEmpty())
    }

    @Test fun limitationTextIsHonest() {
        assertTrue(AppLockPolicy.LIMITATION_TEXT.contains("from My Launcher only"))
        assertTrue(AppLockPolicy.PRIVATE_TEXT.contains("Android Settings"))
    }
}
