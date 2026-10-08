package com.mylauncher.app.data.preferences

/** Encodes ordered lists of package names into a single preference string. */
object ListCodec {
    private const val SEPARATOR = "\n"

    fun encode(list: List<String>): String =
        list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(SEPARATOR)

    fun decode(raw: String?): List<String> =
        raw?.split(SEPARATOR)?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct() ?: emptyList()
}

/** Pure list operations used for favorites. Never produces duplicates. */
object ListOps {
    fun add(list: List<String>, item: String): List<String> =
        if (item in list) list else list + item

    fun remove(list: List<String>, item: String): List<String> =
        list.filterNot { it == item }

    /** Moves [item] by [delta] positions, clamped to the list bounds. */
    fun move(list: List<String>, item: String, delta: Int): List<String> {
        val from = list.indexOf(item)
        if (from < 0) return list
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (to == from) return list
        val mutable = list.toMutableList()
        mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }

    /**
     * Applies a new order for the items the user can currently see, while keeping items that are
     * not visible (hidden or uninstalled apps) exactly where they were.
     */
    fun reorder(existing: List<String>, newVisibleOrder: List<String>): List<String> {
        val visible = newVisibleOrder.distinct()
        val visibleSet = visible.toSet()
        if (existing.count { it in visibleSet } != visible.size) return existing
        var next = 0
        return existing.map { if (it in visibleSet) visible[next++] else it }
    }
}
