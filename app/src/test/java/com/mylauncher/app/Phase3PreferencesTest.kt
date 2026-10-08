package com.mylauncher.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.data.model.BlurLevel
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.OverlayStyle
import com.mylauncher.app.data.model.SearchBarStyle
import com.mylauncher.app.data.model.SurfaceStyle
import com.mylauncher.app.data.model.TextScale
import com.mylauncher.app.data.model.ThemeSettings
import com.mylauncher.app.data.model.WallpaperSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.preferences.LauncherPreferences
import com.mylauncher.app.data.preferences.PrefKeys
import com.mylauncher.app.launcher.search.SearchEngine
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Persistence for the Phase 3 settings, including "an old Phase 2 install keeps working". */
class Phase3PreferencesTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var prefs: LauncherPreferences

    @Before fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "p3.preferences_pb") }
        prefs = LauncherPreferences(store)
    }

    @After fun tearDown() {
        scope.cancel()
    }

    @Test fun phaseTwoDataSurvivesAndPhaseThreeDefaultsApply() = runBlocking {
        // Simulate a Phase 2 install: only legacy keys exist.
        store.edit {
            it[PrefKeys.THEME] = "DARK"
            it[PrefKeys.COLUMNS] = 5
            it[PrefKeys.FAVORITES] = "a\nb\nfolder:f1"
            it[PrefKeys.HIDDEN] = "x"
            it[PrefKeys.FOLDERS] = "f1\tWork\ta"
            it[PrefKeys.ONBOARDING_DONE] = true
        }
        val s = prefs.settings.first()
        assertEquals(ThemeMode.DARK, s.theme)
        assertEquals(5, s.columns)
        assertTrue(s.onboardingDone)
        assertEquals(listOf("a", "b", "folder:f1"), prefs.favorites.first())
        assertEquals(setOf("x"), prefs.hidden.first())
        assertEquals("Work", prefs.folders.first().single().name)
        assertEquals(SmartSettings(), prefs.smartSettings.first())
        assertEquals(CategoryConfig().entries, prefs.categoryConfig.first().entries)
        assertTrue(prefs.categoryOverrides.first().isEmpty())
    }

    @Test fun smartSettingsPersist() = runBlocking {
        val custom = SmartSettings(
            smartSuggestions = false,
            webSearchEnabled = false,
            searchEngine = SearchEngine.CUSTOM,
            customSearchUrl = "https://example.org/?q=%s",
            searchSettingsShortcuts = false,
            groupDrawerByCategory = true,
        )
        prefs.saveSmartSettings(custom)
        assertEquals(custom, prefs.smartSettings.first())
    }

    @Test fun corruptedSearchEngineFallsBackToDefault() = runBlocking {
        store.edit { it[PrefKeys.SEARCH_ENGINE] = "ALTAVISTA" }
        assertEquals(SearchEngine.GOOGLE, prefs.smartSettings.first().searchEngine)
    }

    @Test fun categoryOverridesAndConfigPersist() = runBlocking {
        prefs.setCategoryOverride("a.b", AppCategory.GAMES)
        prefs.setCategoryOverride("c.d", AppCategory.WORK)
        prefs.setCategoryOverride("c.d", null)
        assertEquals(mapOf("a.b" to AppCategory.GAMES), prefs.categoryOverrides.first())

        val config = CategoryConfig().rename(AppCategory.TOOLS, "Utilities").setHidden(AppCategory.OTHER, true).move(AppCategory.GAMES, -5)
        prefs.saveCategoryConfig(config)
        assertEquals(config.normalized(), prefs.categoryConfig.first())

        prefs.resetCategories()
        assertEquals(CategoryConfig().entries, prefs.categoryConfig.first().entries)
        assertTrue(prefs.categoryOverrides.first().isEmpty())
    }

    @Test fun launchesFeedTheLogAndClearingRemovesIt() = runBlocking {
        prefs.recordLaunch("a", 1000L)
        prefs.recordLaunch("b", 2000L)
        assertEquals(listOf("a", "b"), prefs.launchLog.first().map { it.packageName })
        assertEquals(2, prefs.launchStats.first().size)
        prefs.clearLaunchHistory()
        assertTrue(prefs.launchLog.first().isEmpty())
        assertTrue(prefs.launchStats.first().isEmpty())
    }

    @Test fun savingLegacySettingsDoesNotTouchPhaseThreeKeys() = runBlocking {
        prefs.saveSmartSettings(SmartSettings(smartSuggestions = false))
        prefs.saveSettings(LauncherSettings(columns = 6))
        assertFalse(prefs.smartSettings.first().smartSuggestions)
        assertEquals(6, prefs.settings.first().columns)
    }

    @Test fun themeAndWallpaperSettingsPersistAndClamp() = runBlocking {
        assertEquals(ThemeSettings(), prefs.themeSettings.first())
        assertEquals(WallpaperSettings(), prefs.wallpaperSettings.first())
        val theme = ThemeSettings(
            dynamicColor = false, accentColor = 0xFF3567C9.toInt(), surfaceStyle = SurfaceStyle.AMOLED,
            textScale = TextScale.LARGE, searchBarStyle = SearchBarStyle.SQUARE, cornerRadius = 8, drawerOpacity = 80,
        )
        prefs.saveThemeSettings(theme)
        assertEquals(theme, prefs.themeSettings.first())
        val wall = WallpaperSettings(OverlayStyle.LIGHT, 40, BlurLevel.MEDIUM, true)
        prefs.saveWallpaperSettings(wall)
        assertEquals(wall, prefs.wallpaperSettings.first())

        store.edit {
            it[PrefKeys.THEME_RADIUS] = 999
            it[PrefKeys.THEME_DRAWER_OPACITY] = 1
            it[PrefKeys.THEME_SURFACE] = "???"
            it[PrefKeys.WALLPAPER_OVERLAY_PERCENT] = -50
        }
        assertEquals(ThemeSettings.MAX_RADIUS, prefs.themeSettings.first().cornerRadius)
        assertEquals(ThemeSettings.MIN_DRAWER_OPACITY, prefs.themeSettings.first().drawerOpacity)
        assertEquals(SurfaceStyle.DEFAULT, prefs.themeSettings.first().surfaceStyle)
        assertEquals(0, prefs.wallpaperSettings.first().overlayPercent)
    }

    @Test fun newThemeModesPersistAndOldValuesStillParse() = runBlocking {
        prefs.saveSettings(LauncherSettings(theme = ThemeMode.CUSTOM))
        assertEquals(ThemeMode.CUSTOM, prefs.settings.first().theme)
        prefs.saveSettings(LauncherSettings(theme = ThemeMode.DYNAMIC))
        assertEquals(ThemeMode.DYNAMIC, prefs.settings.first().theme)
        store.edit { it[PrefKeys.THEME] = "LIGHT" }
        assertEquals(ThemeMode.LIGHT, prefs.settings.first().theme)
    }

    @Test fun lockedAppsAndPrivateAppsAreIndependentLists() = runBlocking {
        prefs.setAppLocked("a", true)
        prefs.setAppLocked("b", true)
        prefs.setAppLocked("a", true)
        prefs.setAppLocked("b", false)
        assertEquals(setOf("a"), prefs.lockedApps.first())
        prefs.hide("x") // private apps reuse the Phase 2 hidden list
        assertEquals(setOf("x"), prefs.hidden.first())
        assertEquals(setOf("a"), prefs.lockedApps.first())
    }
}
