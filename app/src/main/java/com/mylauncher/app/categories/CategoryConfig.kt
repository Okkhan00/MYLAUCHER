package com.mylauncher.app.categories

import com.mylauncher.app.data.model.AppInfo

/** User customization of one category: optional custom name and whether it is hidden. */
data class CategoryEntry(val category: AppCategory, val customName: String? = null, val hidden: Boolean = false) {
    val displayName: String get() = customName?.takeIf { it.isNotBlank() } ?: category.defaultName
}

/** Ordered list of every category exactly once. Order, names and visibility are user-controlled. */
data class CategoryConfig(val entries: List<CategoryEntry> = defaultEntries()) {

    /** Guarantees each category appears exactly once (missing ones are appended). */
    fun normalized(): CategoryConfig {
        val seen = LinkedHashMap<AppCategory, CategoryEntry>()
        entries.forEach { seen.putIfAbsent(it.category, it) }
        AppCategory.entries.forEach { seen.putIfAbsent(it, CategoryEntry(it)) }
        return CategoryConfig(seen.values.toList())
    }

    fun rename(category: AppCategory, name: String?): CategoryConfig = update(category) {
        it.copy(customName = CategoryCodec.sanitizeName(name))
    }

    fun setHidden(category: AppCategory, hidden: Boolean): CategoryConfig = update(category) { it.copy(hidden = hidden) }

    fun move(category: AppCategory, delta: Int): CategoryConfig {
        val list = normalized().entries.toMutableList()
        val from = list.indexOfFirst { it.category == category }
        if (from < 0) return this
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (to == from) return this
        list.add(to, list.removeAt(from))
        return CategoryConfig(list)
    }

    fun entryFor(category: AppCategory): CategoryEntry =
        normalized().entries.first { it.category == category }

    private fun update(category: AppCategory, f: (CategoryEntry) -> CategoryEntry): CategoryConfig =
        CategoryConfig(normalized().entries.map { if (it.category == category) f(it) else it })

    companion object {
        fun defaultEntries(): List<CategoryEntry> = AppCategory.entries.map { CategoryEntry(it) }
    }
}

/** An app group shown in the drawer. */
data class CategoryGroup(val entry: CategoryEntry, val apps: List<AppInfo>)

object CategoryOrganizer {
    /** The category for [app]: the user's manual choice if any, otherwise the heuristic result. */
    fun categoryOf(app: AppInfo, overrides: Map<String, AppCategory>): AppCategory =
        overrides[app.packageName] ?: AppCategorizer.categorize(app)

    /**
     * Groups [apps] in the user's category order. Empty groups are dropped. Hidden categories are
     * dropped unless [includeHidden] is true, so the All Apps view is always the complete list.
     */
    fun group(
        apps: List<AppInfo>,
        overrides: Map<String, AppCategory>,
        config: CategoryConfig,
        includeHidden: Boolean = false,
    ): List<CategoryGroup> {
        val byCategory = apps.groupBy { categoryOf(it, overrides) }
        return config.normalized().entries
            .filter { includeHidden || !it.hidden }
            .mapNotNull { entry ->
                byCategory[entry.category]?.takeIf { it.isNotEmpty() }
                    ?.let { CategoryGroup(entry, it.sortedBy { app -> app.label.lowercase() }) }
            }
    }
}

/** Persistence format for category settings (kept in the existing DataStore as plain strings). */
object CategoryCodec {
    const val MAX_NAME = 24

    fun sanitizeName(name: String?): String? =
        name?.replace('\t', ' ')?.replace('\n', ' ')?.trim()?.take(MAX_NAME)?.takeIf { it.isNotEmpty() }

    /** One category per line: id TAB customName TAB hidden(0/1). Order of lines is the display order. */
    fun encodeConfig(config: CategoryConfig): String = config.normalized().entries.joinToString("\n") {
        "${it.category.id}\t${it.customName.orEmpty()}\t${if (it.hidden) 1 else 0}"
    }

    fun decodeConfig(raw: String?): CategoryConfig {
        if (raw.isNullOrBlank()) return CategoryConfig()
        val entries = raw.lineSequence().mapNotNull { line ->
            val p = line.split("\t")
            val category = AppCategory.fromId(p.getOrNull(0)) ?: return@mapNotNull null
            CategoryEntry(category, sanitizeName(p.getOrNull(1)), p.getOrNull(2) == "1")
        }.toList()
        return CategoryConfig(entries).normalized()
    }

    /** One override per line: package TAB category id. */
    fun encodeOverrides(map: Map<String, AppCategory>): String =
        map.entries.joinToString("\n") { "${it.key}\t${it.value.id}" }

    fun decodeOverrides(raw: String?): Map<String, AppCategory> {
        if (raw.isNullOrBlank()) return emptyMap()
        val out = LinkedHashMap<String, AppCategory>()
        raw.lineSequence().forEach { line ->
            val p = line.split("\t")
            val category = AppCategory.fromId(p.getOrNull(1))
            if (p.size == 2 && p[0].isNotBlank() && category != null) out[p[0]] = category
        }
        return out
    }
}
