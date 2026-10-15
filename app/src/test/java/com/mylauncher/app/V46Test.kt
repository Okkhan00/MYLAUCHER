package com.mylauncher.app

import com.mylauncher.app.backup.BackupSchema
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.launcher.home.DockOps
import com.mylauncher.app.launcher.home.HomeGridOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V46Test {

    // ---- grid reorder ------------------------------------------------------------------------

    @Test
    fun moveInsertsAtTargetWithoutLosingOrDuplicatingItems() {
        val order = listOf("a", "b", "c", "d", "e")
        val moved = HomeGridOps.move(order, "a", 3)
        assertEquals(listOf("b", "c", "d", "a", "e"), moved)
        assertEquals(order.sorted(), moved.sorted())
        assertEquals(listOf("e", "a", "b", "c", "d"), HomeGridOps.move(order, "e", 0))
    }

    @Test
    fun moveClampsAndIgnoresUnknown() {
        val order = listOf("a", "b", "c")
        assertEquals(listOf("b", "c", "a"), HomeGridOps.move(order, "a", 99))
        assertEquals(listOf("c", "a", "b"), HomeGridOps.move(order, "c", -5))
        assertEquals(order, HomeGridOps.move(order, "zzz", 1))
        assertEquals(order, HomeGridOps.move(order, "b", 1))
        assertEquals(emptyList<String>(), HomeGridOps.move(emptyList(), "a", 0))
    }

    @Test
    fun repeatedMovesKeepAValidPermutation() {
        var order = (1..12).map { "app$it" }
        val original = order.sorted()
        for (i in 0 until 60) order = HomeGridOps.move(order, "app${(i * 7) % 12 + 1}", (i * 5) % 14 - 1)
        assertEquals(original, order.sorted())
        assertEquals(order.size, order.toSet().size)
    }

    @Test
    fun sanitizeDropsUnknownDuplicatesAndAppendsNew() {
        val result = HomeGridOps.sanitize(listOf("b", "x", "a", "b"), listOf("a", "b", "c"))
        assertEquals(listOf("b", "a", "c"), result)
        assertEquals(listOf("a", "b"), HomeGridOps.sanitize(emptyList(), listOf("a", "b", "a")))
        assertEquals(emptyList<String>(), HomeGridOps.sanitize(listOf("x"), emptyList()))
    }

    @Test
    fun gridMathFollowsColumnCount() {
        assertEquals(0, HomeGridOps.rowCount(0, 4))
        assertEquals(3, HomeGridOps.rowCount(9, 4))
        assertEquals(2, HomeGridOps.rowCount(9, 5))
        assertEquals(2, HomeGridOps.rowCount(9, 6))
        assertEquals(1 to 2, HomeGridOps.cellOf(6, 4))
        assertEquals(1 to 1, HomeGridOps.cellOf(6, 5))
        // Out-of-range column counts are clamped, never divide by zero.
        assertEquals(0 to 0, HomeGridOps.cellOf(0, 0))
        assertEquals(4, HomeGridOps.clampColumns(1))
        assertEquals(6, HomeGridOps.clampColumns(99))
    }

    @Test
    fun changingColumnsKeepsOrderAndItemCount() {
        val order = (1..10).map { "app$it" }
        for (columns in 4..6) {
            val cells = order.indices.map { HomeGridOps.cellOf(it, columns) }
            assertEquals(order.size, cells.toSet().size) // no two icons share a cell
            assertTrue(cells.all { it.second < columns })
        }
    }

    @Test
    fun noSaveWhenOrderUnchanged() {
        assertFalse(HomeGridOps.changed(listOf("a", "b"), listOf("a", "b")))
        assertTrue(HomeGridOps.changed(listOf("a", "b"), listOf("b", "a")))
    }

    // ---- dock --------------------------------------------------------------------------------

    @Test
    fun dockAddRespectsCapacityAndDuplicates() {
        var dock = emptyList<String>()
        dock = DockOps.add(dock, "a", 2)
        dock = DockOps.add(dock, "a", 2)
        dock = DockOps.add(dock, "b", 2)
        dock = DockOps.add(dock, "c", 2)
        assertEquals(listOf("a", "b"), dock)
        assertTrue(DockOps.isFull(dock, 2))
        assertEquals(emptyList<String>(), DockOps.add(emptyList(), "a", 0))
        assertEquals(listOf("a"), DockOps.add(emptyList(), "a", 99).take(1))
    }

    @Test
    fun dockMoveRemoveAndPrune() {
        val dock = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), DockOps.move(dock, "a", 1))
        assertEquals(listOf("c", "a", "b"), DockOps.move(dock, "c", -9))
        assertEquals(dock, DockOps.move(dock, "zzz", 1))
        assertEquals(listOf("a", "c"), DockOps.remove(dock, "b"))
        assertEquals(listOf("a", "c"), DockOps.prune(dock, setOf("a", "c", "q")))
    }

    @Test
    fun shrinkingTheDockReturnsExtraApps() {
        val (keep, returned) = DockOps.shrink(listOf("a", "b", "c", "d"), 2)
        assertEquals(listOf("a", "b"), keep)
        assertEquals(listOf("c", "d"), returned)
        val (none, all) = DockOps.shrink(listOf("a", "b"), 0)
        assertEquals(emptyList<String>(), none)
        assertEquals(listOf("a", "b"), all)
        val (same, nothing) = DockOps.shrink(listOf("a"), 5)
        assertEquals(listOf("a"), same)
        assertTrue(nothing.isEmpty())
    }

    // ---- independent settings ----------------------------------------------------------------

    @Test
    fun newSettingsHaveBackwardCompatibleDefaults() {
        val d = LauncherSettings()
        assertEquals(LauncherSettings.DEFAULT_COLUMNS, d.homeColumns)
        assertEquals(IconSize.MEDIUM, d.homeIconSize)
        assertTrue(d.homeShowLabels)
        assertEquals(0, d.dockCount) // no dock unless the user turns it on
    }

    @Test
    fun homeColumnsFallBackToTheValueTheUserAlreadyHad() {
        assertEquals(6, LauncherSettings.resolveColumns(null, 6))
        assertEquals(5, LauncherSettings.resolveColumns(5, 6))
        assertEquals(4, LauncherSettings.resolveColumns(null, null))
        assertEquals(6, LauncherSettings.resolveColumns(99, null))
        assertEquals(4, LauncherSettings.resolveColumns(-3, null))
    }

    @Test
    fun homeAndDrawerSettingsAreIndependent() {
        val base = LauncherSettings()
        val home = base.copy(homeColumns = 6, homeIconSize = IconSize.LARGE, homeShowLabels = false, dockCount = 4)
        assertEquals(base.columns, home.columns)
        assertEquals(base.iconSize, home.iconSize)
        assertEquals(base.showLabels, home.showLabels)
        val drawer = base.copy(columns = 5, iconSize = IconSize.SMALL, showLabels = false)
        assertEquals(base.homeColumns, drawer.homeColumns)
        assertEquals(base.homeIconSize, drawer.homeIconSize)
        assertEquals(base.homeShowLabels, drawer.homeShowLabels)
        assertEquals(base.dockCount, drawer.dockCount)
        assertNotEquals(home, drawer)
    }

    @Test
    fun backupKnowsTheNewKeys() {
        listOf("home_columns", "home_icon_size", "home_show_labels", "dock_count", "dock").forEach {
            assertTrue(it, BackupSchema.keys.containsKey(it))
        }
    }
}
