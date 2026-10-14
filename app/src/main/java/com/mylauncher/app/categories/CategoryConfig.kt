package com.mylauncher.app.categories

import com.mylauncher.app.data.model.AppInfo

/** User customization of one category: optional custom name and whether it is hidden. */
data class CategoryEntry(
    val category: AppCategory,
    val customName: String? = null,
    val hidden: Boolean = false,
    /** Icon chosen when a custom category was created (one of [CategoryVisuals.choices]). */
    val emoji: String? = null,
) {
    val displayName: String get() = customName?.takeIf { it.isNotBlank() } ?: category.defaultName

    /** Built-in categories are always active; a custom slot is active once it has been given a name. */
    val isActive: Boolean get() = !category.isCustom || !customName.isNullOrBlank()
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
        val clean = CategoryCodec.sanitizeName(name)
        // A user-created category cannot lose its name (that would delete it); delete it explicitly instead.
        if (category.isCustom && clean == null) it else it.copy(customName = clean)
    }

    /** Categories that exist: the built-in ones and the custom ones the user created, in display order. */
    val activeEntries: List<CategoryEntry> get() = normalized().entries.filter { it.isActive }

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
        val active = config.normalized().entries.filter { it.isActive }
        val activeCategories = active.mapTo(HashSet()) { it.category }
        // A manual choice that points at a deleted custom category falls back to the automatic category.
        val byCategory = apps.groupBy { app ->
            overrides[app.packageName]?.takeIf { it in activeCategories } ?: AppCategorizer.categorize(app)
        }
        return active
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
        val base = "${it.category.id}\t${it.customName.orEmpty()}\t${if (it.hidden) 1 else 0}"
        // The icon is an optional fourth field, so data saved by earlier versions still reads the same.
        if (it.emoji != null && it.emoji in CategoryVisuals.choices) "$base\t${it.emoji}" else base
    }

    fun decodeConfig(raw: String?): CategoryConfig {
        if (raw.isNullOrBlank()) return CategoryConfig()
        val entries = raw.lineSequence().mapNotNull { line ->
            val p = line.split("\t")
            val category = AppCategory.fromId(p.getOrNull(0)) ?: return@mapNotNull null
            CategoryEntry(
                category,
                sanitizeName(p.getOrNull(1)),
                p.getOrNull(2) == "1",
                p.getOrNull(3)?.takeIf { it in CategoryVisuals.choices },
            )
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

/** Rules for categories the user creates. Pure, so they are unit tested. */
object CategoryRules {
    val MAX_CUSTOM: Int = AppCategory.entries.count { it.isCustom }

    sealed interface CreateResult {
        data class Ok(val config: CategoryConfig, val category: AppCategory) : CreateResult
        data class Rejected(val message: String) : CreateResult
    }

    /** True when [name] (case-insensitive) is already the name of an existing category other than [except]. */
    fun nameTaken(config: CategoryConfig, name: String, except: AppCategory? = null): Boolean {
        val wanted = name.trim()
        return config.activeEntries.any { it.category != except && it.displayName.equals(wanted, ignoreCase = true) }
    }

    fun create(config: CategoryConfig, rawName: String, emoji: String? = null): CreateResult {
        val name = CategoryCodec.sanitizeName(rawName) ?: return CreateResult.Rejected("Enter a name for the category.")
        if (nameTaken(config, name)) return CreateResult.Rejected("A category called \"$name\" already exists.")
        val normalized = config.normalized()
        val slot = normalized.entries.firstOrNull { it.category.isCustom && !it.isActive }
            ?: return CreateResult.Rejected("You can create up to $MAX_CUSTOM categories of your own.")
        val created = slot.copy(
            customName = name,
            hidden = false,
            emoji = emoji?.takeIf { it in CategoryVisuals.choices },
        )
        // New categories go to the end of the list, after everything the user already arranged.
        val entries = normalized.entries.filterNot { it.category == slot.category } + created
        return CreateResult.Ok(CategoryConfig(entries), slot.category)
    }

    /** Frees a user-created category's slot. Built-in categories cannot be deleted. */
    fun delete(config: CategoryConfig, category: AppCategory): CategoryConfig {
        if (!category.isCustom) return config
        return CategoryConfig(config.normalized().entries.map {
            if (it.category == category) CategoryEntry(category) else it
        })
    }
}
