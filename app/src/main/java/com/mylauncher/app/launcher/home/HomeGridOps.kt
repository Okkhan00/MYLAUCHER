package com.mylauncher.app.launcher.home

/**
 * Pure helpers for the home grid and the dock. Nothing here touches Android, so it is unit tested.
 * The home screen is one ordered list that the grid flows into rows; there are no free coordinates.
 */
object HomeGridOps {
    const val MIN_COLUMNS = 4
    const val MAX_COLUMNS = 6
    const val DEFAULT_COLUMNS = 4
    const val MAX_DOCK = 5

    fun clampColumns(value: Int): Int = value.coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    fun clampDock(value: Int): Int = value.coerceIn(0, MAX_DOCK)

    /**
     * Moves [key] so it ends up at [to] (clamped). Unknown keys or a no-op move return the list unchanged.
     * The result never has duplicates and always has the same items as the input.
     */
    fun move(order: List<String>, key: String, to: Int): List<String> {
        val from = order.indexOf(key)
        if (from < 0) return order
        val target = to.coerceIn(0, order.lastIndex)
        if (target == from) return order
        val result = order.toMutableList()
        result.removeAt(from)
        result.add(target, key)
        return result
    }

    /**
     * Cleans a saved order against the items that really exist: unknown and duplicate entries are
     * dropped, the remaining order is kept, and valid items that were not saved yet are appended.
     */
    fun sanitize(saved: List<String>, valid: List<String>): List<String> {
        val validSet = valid.toSet()
        val kept = saved.filter { it in validSet }.distinct()
        val keptSet = kept.toSet()
        return kept + valid.filter { it !in keptSet }.distinct()
    }

    fun rowCount(itemCount: Int, columns: Int): Int {
        if (itemCount <= 0) return 0
        val c = clampColumns(columns)
        return (itemCount + c - 1) / c
    }

    /** Row and column (both 0-based) of the item at [index] for a grid with [columns] columns. */
    fun cellOf(index: Int, columns: Int): Pair<Int, Int> {
        val c = clampColumns(columns)
        return (index / c) to (index % c)
    }

    /** What the home grid shows after a drag was released: nothing is saved when the order did not change. */
    fun changed(before: List<String>, after: List<String>): Boolean = before != after
}

/** The dock: a short ordered row of apps (no folders). Capacity comes from the dock size setting. */
object DockOps {

    /** Keeps only apps that are still valid, so uninstalled or hidden apps never fill a slot. */
    fun prune(dock: List<String>, valid: Set<String>): List<String> =
        dock.filter { it in valid }.distinct()

    fun isFull(dock: List<String>, capacity: Int): Boolean = dock.size >= HomeGridOps.clampDock(capacity)

    /** Adds [pkg] at the end. Duplicates and a full dock leave the list unchanged. */
    fun add(dock: List<String>, pkg: String, capacity: Int): List<String> {
        if (pkg in dock || isFull(dock, capacity)) return dock
        return dock + pkg
    }

    fun remove(dock: List<String>, pkg: String): List<String> = dock.filterNot { it == pkg }

    fun move(dock: List<String>, pkg: String, delta: Int): List<String> {
        val from = dock.indexOf(pkg)
        if (from < 0) return dock
        return HomeGridOps.move(dock, pkg, from + delta)
    }

    /**
     * When the dock gets smaller, apps that no longer fit go back to the home grid instead of
     * disappearing. Returns the new dock and the apps to return.
     */
    fun shrink(dock: List<String>, capacity: Int): Pair<List<String>, List<String>> {
        val cap = HomeGridOps.clampDock(capacity)
        return dock.take(cap) to dock.drop(cap)
    }
}
