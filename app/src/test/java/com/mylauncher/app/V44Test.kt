package com.mylauncher.app

import com.mylauncher.app.about.VersionHistory
import com.mylauncher.app.categories.CategorySwipe
import com.mylauncher.app.data.AppListOps
import com.mylauncher.app.data.PackageEvents
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.SmartSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class V44Test {
    // ---- package-change handling ----
    @Test fun installAndUpdateAreRefreshedAndVerified() {
        assertEquals(PackageEvents.Plan.REFRESH_AND_VERIFY, PackageEvents.plan(PackageEvents.ADDED, false))
        assertEquals(PackageEvents.Plan.REFRESH_AND_VERIFY, PackageEvents.plan(PackageEvents.REPLACED, false))
    }

    @Test fun removalRefreshesButTheRemovedHalfOfAnUpdateIsIgnored() {
        assertEquals(PackageEvents.Plan.REFRESH, PackageEvents.plan(PackageEvents.REMOVED, false))
        assertEquals(PackageEvents.Plan.IGNORE, PackageEvents.plan(PackageEvents.REMOVED, true))
        assertEquals(PackageEvents.Plan.REFRESH, PackageEvents.plan(PackageEvents.FULLY_REMOVED, false))
    }

    @Test fun enableDisableRefreshesAndUnknownActionsAreIgnored() {
        assertEquals(PackageEvents.Plan.REFRESH, PackageEvents.plan(PackageEvents.CHANGED, false))
        assertEquals(PackageEvents.Plan.IGNORE, PackageEvents.plan("android.intent.action.SOMETHING", false))
        assertEquals(PackageEvents.Plan.IGNORE, PackageEvents.plan(null, false))
    }

    private fun app(pkg: String, label: String = pkg) = AppInfo(pkg, "$pkg.Main", label)

    @Test fun newAppAppearsSortedAndOthersKeepTheirOrder() {
        val before = AppListOps.normalize(listOf(app("a", "Alpha"), app("c", "Charlie")))
        val after = AppListOps.replacePackage(before, "b", listOf(app("b", "Bravo")))
        assertEquals(listOf("a", "b", "c"), after.map { it.packageName })
    }

    @Test fun uninstalledAppDisappearsAndNothingIsDuplicated() {
        val before = AppListOps.normalize(listOf(app("a"), app("b")))
        assertEquals(listOf("a"), AppListOps.replacePackage(before, "b", emptyList()).map { it.packageName })
        // Adding the same package twice never duplicates it.
        val twice = AppListOps.replacePackage(before, "b", listOf(app("b"), app("b")))
        assertEquals(2, twice.size)
    }

    @Test fun updatedLabelReplacesTheOldEntryAndUnchangedPackageIsTheSameList() {
        val before = AppListOps.normalize(listOf(app("a", "Old name")))
        val renamed = AppListOps.replacePackage(before, "a", listOf(app("a", "New name")))
        assertEquals("New name", renamed.single().label)
        assertSame(before, AppListOps.replacePackage(before, "a", listOf(app("a", "Old name"))))
    }

    // ---- category swipe ----
    private val ids = listOf("social", "games", "tools")

    @Test fun swipeLeftGoesToNextAndRightToPrevious() {
        assertEquals("tools", CategorySwipe.target(ids, "games", -200f, 100f))
        assertEquals("social", CategorySwipe.target(ids, "games", 200f, 100f))
    }

    @Test fun firstAndLastCategoryHaveNothingBeyondThem() {
        assertNull(CategorySwipe.target(ids, "social", 200f, 100f))
        assertNull(CategorySwipe.target(ids, "tools", -200f, 100f))
    }

    @Test fun shortDragsAndUnknownCategoriesDoNothing() {
        assertNull(CategorySwipe.target(ids, "games", -99f, 100f))
        assertNull(CategorySwipe.target(ids, "games", 50f, 100f))
        assertNull(CategorySwipe.target(ids, "missing", -300f, 100f))
        assertNull(CategorySwipe.target(emptyList(), "games", -300f, 100f))
    }

    // ---- settings defaults and About ----
    @Test fun recentAndSuggestionsAreOnByDefault() {
        val s = SmartSettings()
        assertTrue(s.showRecentApps)
        assertTrue(s.smartSuggestions)
    }

    @Test fun versionHistoryIsNewestFirstAndEveryEntryHasContent() {
        val releases = VersionHistory.releases
        assertEquals("4.4", releases.first().version)
        assertTrue(releases.all { it.points.isNotEmpty() && it.title.isNotBlank() })
        assertEquals(releases.map { it.version }.distinct(), releases.map { it.version })
    }
}
