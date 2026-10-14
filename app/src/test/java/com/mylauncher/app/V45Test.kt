package com.mylauncher.app

import com.mylauncher.app.bulk.BulkPlan
import com.mylauncher.app.bulk.BulkSelection
import com.mylauncher.app.bulk.UninstallItem
import com.mylauncher.app.bulk.UninstallOutcome
import com.mylauncher.app.bulk.UninstallRules
import com.mylauncher.app.bulk.UninstallSession
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryCodec
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.categories.CategoryOrganizer
import com.mylauncher.app.categories.CategoryRules
import com.mylauncher.app.categories.CategoryVisuals
import com.mylauncher.app.data.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V45Test {
    private fun app(pkg: String, label: String = pkg) = AppInfo(pkg, "$pkg.Main", label)

    // ---- selection ----
    @Test fun selectingTheSameAppTwiceNeverDuplicatesAndToggleRemoves() {
        var s = BulkSelection().add("a").add("a").add("b")
        assertEquals(listOf("a", "b"), s.packages)
        s = s.toggle("a")
        assertEquals(listOf("b"), s.packages)
        s = s.toggle("c")
        assertEquals(2, s.count)
    }

    @Test fun selectAllClearAndRetain() {
        val all = listOf("a", "b", "c", "b")
        val s = BulkSelection().selectAll(all)
        assertEquals(listOf("a", "b", "c"), s.packages)
        assertTrue(s.allSelected(all))
        assertEquals(listOf("a", "c"), s.retainOnly(setOf("a", "c", "z")).packages)
        assertTrue(s.clear().isEmpty)
        assertFalse(BulkSelection().allSelected(emptyList()))
    }

    @Test fun retainOnlyKeepsTheSameInstanceWhenNothingChanged() {
        val s = BulkSelection(listOf("a", "b"))
        assertTrue(s === s.retainOnly(setOf("a", "b", "c")))
    }

    // ---- bulk plans ----
    @Test fun favoritesPlanSkipsAppsAlreadyOnHome() {
        val home = listOf("a", "folder:1", "c")
        assertEquals(listOf("b", "d"), BulkPlan.favoritesToAdd(listOf("a", "b", "d"), home))
        assertEquals(listOf("a", "c"), BulkPlan.favoritesToRemove(listOf("a", "b", "c"), home))
    }

    @Test fun mixedHideAndLockSelectionsSplitConsistently() {
        val sel = listOf("a", "b", "c")
        assertEquals(listOf("a", "c"), BulkPlan.hideTargets(sel, setOf("b")))
        assertEquals(listOf("b"), BulkPlan.unhideTargets(sel, setOf("b")))
        assertEquals(listOf("b", "c"), BulkPlan.lockTargets(sel, setOf("a")))
        assertEquals(listOf("a"), BulkPlan.unlockTargets(sel, setOf("a")))
        assertEquals(listOf("c"), BulkPlan.inFolder(sel, setOf("c", "z")))
        assertEquals(listOf("a"), BulkPlan.withManualCategory(sel, setOf("a")))
    }

    // ---- user-created categories ----
    @Test fun createCategoryUsesAFreeSlotGoesToTheEndAndKeepsOrder() {
        val base = CategoryConfig().move(AppCategory.TOOLS, -100)
        val r = CategoryRules.create(base, "  Kids  ", CategoryVisuals.choices[2]) as CategoryRules.CreateResult.Ok
        assertEquals(AppCategory.CUSTOM_1, r.category)
        assertEquals("Kids", r.config.entryFor(AppCategory.CUSTOM_1).displayName)
        assertEquals(CategoryVisuals.choices[2], r.config.entryFor(AppCategory.CUSTOM_1).emoji)
        assertEquals(AppCategory.TOOLS, r.config.activeEntries.first().category)
        assertEquals(AppCategory.CUSTOM_1, r.config.activeEntries.last().category)
        // Second one takes the next slot.
        val r2 = CategoryRules.create(r.config, "Work stuff") as CategoryRules.CreateResult.Ok
        assertEquals(AppCategory.CUSTOM_2, r2.category)
    }

    @Test fun blankAndDuplicateNamesAreRejected() {
        val config = CategoryConfig()
        assertTrue(CategoryRules.create(config, "   ") is CategoryRules.CreateResult.Rejected)
        assertTrue(CategoryRules.create(config, "games") is CategoryRules.CreateResult.Rejected) // built-in name
        val ok = CategoryRules.create(config, "Kids") as CategoryRules.CreateResult.Ok
        assertTrue(CategoryRules.create(ok.config, "KIDS") is CategoryRules.CreateResult.Rejected)
    }

    @Test fun categoryLimitIsReportedClearly() {
        var config = CategoryConfig()
        repeat(CategoryRules.MAX_CUSTOM) { i ->
            config = (CategoryRules.create(config, "Cat ${i + 1}") as CategoryRules.CreateResult.Ok).config
        }
        val r = CategoryRules.create(config, "One too many")
        assertTrue(r is CategoryRules.CreateResult.Rejected)
    }

    @Test fun unusedCustomSlotsAreInvisibleAndBuiltInsAreUnchanged() {
        val config = CategoryConfig()
        assertEquals(12, config.activeEntries.size)
        assertTrue(config.activeEntries.none { it.category.isCustom })
        val groups = CategoryOrganizer.group(listOf(app("com.whatsapp", "WhatsApp")), emptyMap(), config)
        assertEquals(listOf(AppCategory.COMMUNICATION), groups.map { it.entry.category })
    }

    @Test fun appsAssignedToACustomCategoryAppearThereAndCountsUpdate() {
        val ok = CategoryRules.create(CategoryConfig(), "Kids") as CategoryRules.CreateResult.Ok
        val apps = listOf(app("com.whatsapp", "WhatsApp"), app("com.instagram.android", "Instagram"))
        val overrides = mapOf("com.whatsapp" to ok.category, "com.instagram.android" to ok.category)
        val groups = CategoryOrganizer.group(apps, overrides, ok.config)
        assertEquals(1, groups.size)
        assertEquals("Kids", groups.single().entry.displayName)
        assertEquals(2, groups.single().apps.size)
    }

    @Test fun deletingACustomCategoryReturnsItsAppsToAutomatic() {
        val ok = CategoryRules.create(CategoryConfig(), "Kids") as CategoryRules.CreateResult.Ok
        val deleted = CategoryRules.delete(ok.config, ok.category)
        assertFalse(deleted.entryFor(ok.category).isActive)
        // A stale manual choice pointing at the deleted slot falls back to the automatic category.
        val groups = CategoryOrganizer.group(listOf(app("com.whatsapp", "WhatsApp")), mapOf("com.whatsapp" to ok.category), deleted)
        assertEquals(listOf(AppCategory.COMMUNICATION), groups.map { it.entry.category })
        // Built-in categories cannot be deleted.
        assertEquals(CategoryConfig(), CategoryRules.delete(CategoryConfig(), AppCategory.GAMES))
    }

    @Test fun renamingACustomCategoryToBlankKeepsItsName() {
        val ok = CategoryRules.create(CategoryConfig(), "Kids") as CategoryRules.CreateResult.Ok
        assertEquals("Kids", ok.config.rename(ok.category, "  ").entryFor(ok.category).displayName)
        assertEquals("Little ones", ok.config.rename(ok.category, "Little ones").entryFor(ok.category).displayName)
    }

    @Test fun customCategoriesSurviveSaveAndLoad() {
        val ok = CategoryRules.create(CategoryConfig(), "Kids", CategoryVisuals.choices[1]) as CategoryRules.CreateResult.Ok
        val loaded = CategoryCodec.decodeConfig(CategoryCodec.encodeConfig(ok.config))
        assertEquals(ok.config.normalized(), loaded)
        assertEquals("Kids", loaded.entryFor(ok.category).displayName)
        // Config saved by an older version (3 fields) still loads.
        assertEquals("Mine", CategoryCodec.decodeConfig("media\tMine\t0").entryFor(AppCategory.MEDIA).displayName)
        assertNull(CategoryCodec.decodeConfig("media\tMine\t0\tnot-an-icon").entryFor(AppCategory.MEDIA).emoji)
    }

    @Test fun everyCategoryIncludingCustomSlotsHasItsOwnIcon() {
        val icons = AppCategory.entries.map { CategoryVisuals.emoji(it) }
        assertEquals(icons.size, icons.toSet().size)
    }

    // ---- bulk uninstall ----
    private val a = UninstallItem("pkg.a", "A")
    private val b = UninstallItem("pkg.b", "B")
    private val c = UninstallItem("pkg.c", "C")

    @Test fun launcherAndSystemAppsAreNeverOfferedForUninstall() {
        assertEquals("My Launcher cannot uninstall itself", UninstallRules.ineligibleReason("me", "me", false))
        assertTrue(UninstallRules.ineligibleReason("sys", "me", true)!!.contains("System"))
        assertNull(UninstallRules.ineligibleReason("user.app", "me", false))
    }

    @Test fun packageCheckDecidesTheOutcome() {
        assertEquals(UninstallOutcome.REMOVED, UninstallRules.classify(0, stillInstalled = false)) // wrong code, but gone
        assertEquals(UninstallOutcome.CANCELLED, UninstallRules.classify(UninstallRules.RESULT_CANCELED, true))
        assertEquals(UninstallOutcome.FAILED, UninstallRules.classify(UninstallRules.RESULT_FIRST_USER, true))
        assertEquals(UninstallOutcome.STILL_INSTALLED, UninstallRules.classify(-1, true))
        assertEquals(UninstallOutcome.STILL_INSTALLED, UninstallRules.classify(null, true))
    }

    @Test fun uninstallSessionGoesOneAppAtATimeAndReportsEach() {
        var s = UninstallSession.plan(listOf(a, b, c, a)) { if (it == c) "System apps can't be uninstalled here" else null }
        assertEquals(listOf(a, b), s.queue)
        assertEquals(1, s.skipped.size)
        assertEquals(a, s.next)
        s = s.record(UninstallOutcome.REMOVED)
        assertEquals(b, s.next)
        assertEquals(2, s.position)
        s = s.record(UninstallOutcome.CANCELLED)
        assertTrue(s.finished)
        assertEquals(listOf(a), s.removed())
        assertEquals(listOf(b), s.cancelled())
        assertTrue(s.notAttempted.isEmpty())
    }

    @Test fun stoppingLeavesTheRestUntouchedAndUnreportedAsRemoved() {
        var s = UninstallSession.plan(listOf(a, b, c)) { null }
        s = s.record(UninstallOutcome.FAILED).stop()
        assertTrue(s.finished)
        assertEquals(listOf(b, c), s.notAttempted)
        assertEquals(listOf(a), s.failed())
        assertTrue(s.removed().isEmpty())
    }

    @Test fun emptyOrAllIneligibleSessionIsImmediatelyFinished() {
        assertTrue(UninstallSession.plan(emptyList()) { null }.finished)
        assertTrue(UninstallSession.plan(listOf(a)) { "no" }.finished)
    }
}
