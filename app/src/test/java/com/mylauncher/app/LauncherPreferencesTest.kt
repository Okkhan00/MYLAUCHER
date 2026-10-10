package com.mylauncher.app

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.mylauncher.app.data.model.ClockStyle
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.GestureAction
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.data.preferences.LauncherPreferences
import com.mylauncher.app.data.preferences.PrefKeys
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

class LauncherPreferencesTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var scope: CoroutineScope
    private lateinit var store: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    private lateinit var prefs: LauncherPreferences

    @Before fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "test.preferences_pb") }
        prefs = LauncherPreferences(store)
    }

    @After fun tearDown() {
        scope.cancel()
    }

    @Test fun defaultsAreUsedWhenNothingIsStored() = runBlocking {
        assertEquals(LauncherSettings(), prefs.settings.first())
        assertTrue(prefs.favorites.first().isEmpty())
        assertTrue(prefs.hidden.first().isEmpty())
    }

    @Test fun settingsArePersisted() = runBlocking {
        val custom = LauncherSettings(
            theme = ThemeMode.DARK,
            iconSize = IconSize.LARGE,
            columns = 5,
            showLabels = false,
            animations = false,
            clockStyle = ClockStyle.MINIMAL,
            showDate = false,
            showSearchBar = false,
            drawerLayout = DrawerLayout.LIST,
            swipeUp = GestureAction.QUICK_ACTIONS,
            swipeDown = GestureAction.NONE,
            doubleTap = GestureAction.OPEN_SETTINGS,
            showBattery = true,
            trackLaunches = false,
            onboardingDone = true,
        )
        prefs.saveSettings(custom)
        assertEquals(custom, prefs.settings.first())
    }

    @Test fun corruptedEnumValuesFallBackToDefaults() = runBlocking {
        store.edit {
            it[PrefKeys.THEME] = "NOT_A_THEME"
            it[PrefKeys.CLOCK_STYLE] = ""
            it[PrefKeys.SWIPE_UP] = "???"
        }
        val s = prefs.settings.first()
        assertEquals(ThemeMode.SYSTEM, s.theme)
        assertEquals(ClockStyle.DIGITAL, s.clockStyle)
        assertEquals(GestureAction.OPEN_DRAWER, s.swipeUp)
    }

    @Test fun gestureDefaultsMatchPhaseOneBehaviour() = runBlocking {
        val s = prefs.settings.first()
        assertEquals(GestureAction.OPEN_DRAWER, s.swipeUp)
        assertEquals(GestureAction.OPEN_SEARCH, s.swipeDown)
        assertEquals(GestureAction.OPEN_SEARCH, s.doubleTap)
    }

    @Test fun outOfRangeColumnsAreClamped() = runBlocking {
        store.edit { it[PrefKeys.COLUMNS] = 99 }
        assertEquals(LauncherSettings.MAX_COLUMNS, prefs.settings.first().columns)
        store.edit { it[PrefKeys.COLUMNS] = 1 }
        assertEquals(LauncherSettings.MIN_COLUMNS, prefs.settings.first().columns)
    }

    @Test fun favoritesKeepOrderAndNeverDuplicate() = runBlocking {
        prefs.addFavorite("a")
        prefs.addFavorite("b")
        prefs.addFavorite("a")
        prefs.addFavorite("c")
        assertEquals(listOf("a", "b", "c"), prefs.favorites.first())

        prefs.moveFavorite("c", -2)
        assertEquals(listOf("c", "a", "b"), prefs.favorites.first())

        prefs.removeFavorite("a")
        assertEquals(listOf("c", "b"), prefs.favorites.first())
    }

    @Test fun hidingRemovesFromFavoritesAndUnhidingRestoresVisibility() = runBlocking {
        prefs.addFavorite("a")
        prefs.addFavorite("b")
        prefs.hide("a")
        assertEquals(setOf("a"), prefs.hidden.first())
        assertEquals(listOf("b"), prefs.favorites.first())

        prefs.unhide("a")
        assertFalse("a" in prefs.hidden.first())
    }

    @Test fun hidingTwiceKeepsOneEntry() = runBlocking {
        prefs.hide("a")
        prefs.hide("a")
        assertEquals(setOf("a"), prefs.hidden.first())
    }

    @Test fun foldersAreCreatedOnHomeAndKeepAppsUnique() = runBlocking {
        prefs.addFavorite("a")
        prefs.addFavorite("b")
        val id = prefs.createFolder("Work", id = "f1")
        assertEquals(listOf("a", "b", "folder:f1"), prefs.favorites.first())

        prefs.addAppToFolder("f1", "a")
        assertEquals(listOf("b", "folder:f1"), prefs.favorites.first())
        assertEquals(listOf("a"), prefs.folders.first().single { it.id == id }.appIds)

        // Putting the app back on the home screen takes it out of the folder.
        prefs.addFavorite("a")
        assertTrue(prefs.folders.first().single().appIds.isEmpty())
        assertEquals(listOf("b", "folder:f1", "a"), prefs.favorites.first())
    }

    @Test fun addingToUnknownFolderLeavesHomeUntouched() = runBlocking {
        prefs.addFavorite("a")
        prefs.addAppToFolder("missing", "a")
        assertEquals(listOf("a"), prefs.favorites.first())
    }

    @Test fun renamingAndDeletingFolders() = runBlocking {
        prefs.createFolder("Old", id = "f1")
        prefs.renameFolder("f1", "New")
        assertEquals("New", prefs.folders.first().single().name)
        prefs.deleteFolder("f1")
        assertTrue(prefs.folders.first().isEmpty())
        assertTrue(prefs.favorites.first().isEmpty())
    }

    @Test fun reorderedHomeListKeepsHiddenItemsInPlace() = runBlocking {
        prefs.addFavorite("a")
        prefs.addFavorite("x")
        prefs.addFavorite("b")
        prefs.setFavoritesOrder(listOf("b", "a"))
        assertEquals(listOf("b", "x", "a"), prefs.favorites.first())
    }

    @Test fun folderOrderPersists() = runBlocking {
        prefs.createFolder("F", id = "f1")
        listOf("a", "b", "c").forEach { prefs.addAppToFolder("f1", it) }
        prefs.setFolderOrder("f1", listOf("c", "a", "b"))
        assertEquals(listOf("c", "a", "b"), prefs.folders.first().single().appIds)
        prefs.moveAppInFolder("f1", "b", -1)
        assertEquals(listOf("c", "b", "a"), prefs.folders.first().single().appIds)
        prefs.removeAppFromFolder("f1", "c")
        assertEquals(listOf("b", "a"), prefs.folders.first().single().appIds)
    }

    @Test fun securityDefaultsAndPinLifecycle() = runBlocking {
        assertFalse(prefs.security.first().lockEnabled)
        assertTrue(prefs.security.first().protectSettings)
        assertTrue(prefs.security.first().protectHidden)

        prefs.setPin("hash", "salt")
        prefs.saveSecurityOptions(biometric = true, protectSettings = false, protectHidden = true)
        val sec = prefs.security.first()
        assertTrue(sec.lockEnabled)
        assertTrue(sec.biometricEnabled)
        assertFalse(sec.protectSettings)

        prefs.clearPin()
        val cleared = prefs.security.first()
        assertFalse(cleared.lockEnabled)
        assertFalse(cleared.biometricEnabled)
    }

    @Test fun launchHistoryIsRecordedAndCanBeCleared() = runBlocking {
        prefs.recordLaunch("a", 10L)
        prefs.recordLaunch("a", 20L)
        prefs.recordLaunch("b", 30L)
        val stats = prefs.launchStats.first()
        assertEquals(2, stats.getValue("a").count)
        assertEquals(20L, stats.getValue("a").lastLaunchedMs)
        prefs.clearLaunchHistory()
        assertTrue(prefs.launchStats.first().isEmpty())
    }
}
