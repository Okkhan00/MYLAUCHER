package com.mylauncher.app

import com.mylauncher.app.data.model.CustomTheme
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SurfaceStyle
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.ui.theme.AccentPalette
import com.mylauncher.app.ui.theme.BuiltInThemes
import com.mylauncher.app.ui.theme.ColorMath
import com.mylauncher.app.ui.theme.CustomThemeCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInThemesTest {
    @Test fun idsAreUniqueAndEightRequestedThemesExist() {
        val all = BuiltInThemes.all
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (n in listOf("Midnight Dark", "Clean Light", "Ocean Blue", "Forest Green", "Royal Purple", "Warm Amber", "Minimal Grey", "AMOLED Black")) {
            assertTrue(n, all.any { it.name == n })
        }
    }

    @Test fun untouchedInstallShowsDefaultSelected() {
        assertEquals(BuiltInThemes.DEFAULT_ID, BuiltInThemes.selectedId(LauncherSettings().theme, ThemeSettings()))
    }

    @Test fun applyingEachBuiltInIsReportedAsSelected() {
        for (t in BuiltInThemes.all) {
            val (s, th) = BuiltInThemes.apply(t, LauncherSettings(), ThemeSettings())
            assertEquals(t.id, BuiltInThemes.selectedId(s.theme, th))
        }
    }

    @Test fun applyingAThemeKeepsShapeTextAndDrawerChoicesAndCustomSlot() {
        val mine = CustomTheme(ThemeMode.DARK, 0xFFAA0000.toInt(), SurfaceStyle.TINTED)
        val before = ThemeSettings(cornerRadius = 8, drawerOpacity = 70, custom = mine)
        val (_, after) = BuiltInThemes.apply(BuiltInThemes.byId("ocean")!!, LauncherSettings(), before)
        assertEquals(8, after.cornerRadius)
        assertEquals(70, after.drawerOpacity)
        assertEquals(mine, after.custom)
    }

    @Test fun customThemeNeverChangesBuiltInDefinitions() {
        val snapshot = BuiltInThemes.all.toList()
        val custom = CustomTheme(ThemeMode.LIGHT, 0xFF123456.toInt(), SurfaceStyle.TINTED)
        BuiltInThemes.applyCustom(custom, LauncherSettings(), ThemeSettings())
        assertEquals(snapshot, BuiltInThemes.all)
    }

    @Test fun customThemeIsSelectedAsCustomAndReturningToBuiltInWorks() {
        val custom = CustomTheme(ThemeMode.LIGHT, 0xFF123456.toInt(), SurfaceStyle.TINTED)
        val (s, th) = BuiltInThemes.applyCustom(custom, LauncherSettings(), ThemeSettings())
        assertEquals(BuiltInThemes.CUSTOM_ID, BuiltInThemes.selectedId(s.theme, th))
        assertEquals(custom, th.custom)
        val (s2, th2) = BuiltInThemes.apply(BuiltInThemes.byId("midnight")!!, s, th)
        assertEquals("midnight", BuiltInThemes.selectedId(s2.theme, th2))
        assertEquals(custom, th2.custom) // the custom slot survives
    }

    @Test fun editingStartsFromTheLookOnScreen() {
        val (s, th) = BuiltInThemes.apply(BuiltInThemes.byId("forest")!!, LauncherSettings(), ThemeSettings())
        val start = BuiltInThemes.currentAsCustom(s.theme, th)
        assertEquals(0xFF2E7D32.toInt(), start.accentColor)
        assertEquals(ThemeMode.CUSTOM, start.mode)
        // Default theme has no accent: editing falls back to the saved custom accent.
        val fromDefault = BuiltInThemes.currentAsCustom(ThemeMode.SYSTEM, ThemeSettings())
        assertEquals(ThemeSettings().custom.accentColor, fromDefault.accentColor)
    }

    @Test fun legacyMixOfSettingsIsCustom() {
        assertEquals(
            BuiltInThemes.CUSTOM_ID,
            BuiltInThemes.selectedId(ThemeMode.LIGHT, ThemeSettings(accentColor = 0xFF010203.toInt())),
        )
    }

    @Test fun customThemeCodecRoundTripAndBadInput() {
        val c = CustomTheme(ThemeMode.DYNAMIC, 0xFF00AA55.toInt(), SurfaceStyle.AMOLED)
        assertEquals(c, CustomThemeCodec.decode(CustomThemeCodec.encode(c)))
        val d = CustomTheme()
        assertEquals(d, CustomThemeCodec.decode(null))
        assertEquals(d, CustomThemeCodec.decode("garbage"))
        assertEquals(d.mode, CustomThemeCodec.decode("SYSTEM,123,DEFAULT").mode)
        assertEquals(d.surfaceStyle, CustomThemeCodec.decode("DARK,123,NOPE").surfaceStyle)
    }

    @Test fun everyBuiltInIsReadableInLightAndDark() {
        for (t in BuiltInThemes.all) {
            if (t.accent == ThemeSettings.NO_ACCENT) continue
            for (dark in listOf(false, true)) {
                val c = AccentPalette.derive(t.accent, dark)
                val tag = "${t.name} dark=$dark"
                assertTrue("$tag text", ColorMath.contrastRatio(c.onSurface, c.surface) >= 7.0)
                assertTrue("$tag muted text", ColorMath.contrastRatio(c.onSurfaceVariant, c.surfaceVariant) >= 4.5)
                assertTrue("$tag button", ColorMath.contrastRatio(c.onPrimary, c.primary) >= 4.5)
                assertTrue("$tag accent text on surface", ColorMath.contrastRatio(c.primary, c.surface) >= 3.0)
                assertTrue("$tag card text", ColorMath.contrastRatio(c.onSurface, c.surfaceContainerHigh) >= 4.5)
                assertTrue("$tag container text", ColorMath.contrastRatio(c.onPrimaryContainer, c.primaryContainer) >= 4.5)
            }
        }
    }

    @Test fun previewUsesPureBlackForAmoledOnlyWhenDark() {
        val t = BuiltInThemes.byId("amoled")!!
        assertEquals(ColorMath.BLACK, BuiltInThemes.preview(t.mode, t.accent, t.surface, systemDark = false).background)
        val tinted = BuiltInThemes.preview(ThemeMode.LIGHT, t.accent, SurfaceStyle.AMOLED, systemDark = true)
        assertNotEquals(ColorMath.BLACK, tinted.background)
    }

    @Test fun previewFollowsSystemModeForFollowSystemThemes() {
        val o = BuiltInThemes.byId("ocean")!!
        val light = BuiltInThemes.preview(o.mode, o.accent, o.surface, systemDark = false)
        val dark = BuiltInThemes.preview(o.mode, o.accent, o.surface, systemDark = true)
        assertNotEquals(light.background, dark.background)
    }
}
