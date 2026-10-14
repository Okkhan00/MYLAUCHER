package com.mylauncher.app.bulk

/**
 * Pure rules for selecting several apps and acting on them together. Kept free of Android classes so every
 * rule is unit tested on the JVM.
 */

/** The selected apps, by package name. A package can only be selected once; order is the order of selection. */
data class BulkSelection(val packages: List<String> = emptyList()) {
    val count: Int get() = packages.size
    val isEmpty: Boolean get() = packages.isEmpty()

    operator fun contains(pkg: String): Boolean = pkg in packages

    fun toggle(pkg: String): BulkSelection =
        if (pkg in packages) BulkSelection(packages - pkg) else BulkSelection(packages + pkg)

    fun add(pkg: String): BulkSelection = if (pkg in packages) this else BulkSelection(packages + pkg)

    /** Every app of [all] selected (what "Select all" means: the apps currently listed). */
    fun selectAll(all: List<String>): BulkSelection = BulkSelection(all.distinct())

    fun clear(): BulkSelection = BulkSelection()

    /** Drops apps that are no longer listed (uninstalled, hidden meanwhile) so the count never lies. */
    fun retainOnly(valid: Set<String>): BulkSelection {
        val kept = packages.filter { it in valid }
        return if (kept.size == packages.size) this else BulkSelection(kept)
    }

    fun allSelected(all: List<String>): Boolean = all.isNotEmpty() && all.all { it in packages }
}

/** Which of the selected apps each action applies to, so mixed selections give clear, consistent results. */
object BulkPlan {
    /** Apps not yet on the home screen. */
    fun favoritesToAdd(selected: List<String>, homeTokens: List<String>): List<String> {
        val home = homeTokens.toSet()
        return selected.filter { it !in home }
    }

    fun favoritesToRemove(selected: List<String>, homeTokens: List<String>): List<String> {
        val home = homeTokens.toSet()
        return selected.filter { it in home }
    }

    fun hideTargets(selected: List<String>, hidden: Set<String>): List<String> = selected.filter { it !in hidden }
    fun unhideTargets(selected: List<String>, hidden: Set<String>): List<String> = selected.filter { it in hidden }
    fun lockTargets(selected: List<String>, locked: Set<String>): List<String> = selected.filter { it !in locked }
    fun unlockTargets(selected: List<String>, locked: Set<String>): List<String> = selected.filter { it in locked }

    /** Selected apps that currently sit inside a folder. */
    fun inFolder(selected: List<String>, folderApps: Set<String>): List<String> = selected.filter { it in folderApps }

    /** Selected apps with a manual category choice that can be reset to automatic. */
    fun withManualCategory(selected: List<String>, overrides: Set<String>): List<String> = selected.filter { it in overrides }
}

object BulkText {
    fun apps(n: Int): String = if (n == 1) "1 app" else "$n apps"

    fun moved(n: Int, categoryName: String): String = "${apps(n)} moved to $categoryName"
    fun added(n: Int, what: String): String = "${apps(n)} $what"
}

/** What happened to one app in a bulk uninstall. */
enum class UninstallOutcome { REMOVED, CANCELLED, FAILED, STILL_INSTALLED }

data class UninstallItem(val packageName: String, val label: String)

object UninstallRules {
    // Values of Activity.RESULT_CANCELED / RESULT_FIRST_USER (kept literal so this stays pure Kotlin).
    const val RESULT_CANCELED = 0
    const val RESULT_FIRST_USER = 1

    /** Null when the app may be offered for uninstall, otherwise a short reason shown to the user. */
    fun ineligibleReason(packageName: String, ownPackage: String, isSystemApp: Boolean): String? = when {
        packageName == ownPackage -> "My Launcher cannot uninstall itself"
        isSystemApp -> "System apps can't be uninstalled here"
        else -> null
    }

    /**
     * Combines Android's result code with a check of what is really installed. The package check is what
     * counts: some phones report the result code unreliably.
     */
    fun classify(resultCode: Int?, stillInstalled: Boolean): UninstallOutcome = when {
        !stillInstalled -> UninstallOutcome.REMOVED
        resultCode == RESULT_CANCELED -> UninstallOutcome.CANCELLED
        resultCode == RESULT_FIRST_USER -> UninstallOutcome.FAILED
        else -> UninstallOutcome.STILL_INSTALLED
    }
}

/**
 * One bulk uninstall, handled strictly one app at a time. Android always shows its own confirmation for each
 * app, so the session only keeps track of the queue and of what each system dialog reported.
 */
data class UninstallSession(
    val queue: List<UninstallItem>,
    /** Apps that were selected but cannot be uninstalled, with the reason. */
    val skipped: List<Pair<UninstallItem, String>> = emptyList(),
    val results: List<Pair<UninstallItem, UninstallOutcome>> = emptyList(),
    val stopped: Boolean = false,
) {
    /** The app whose system dialog is up next (or currently showing). Null when nothing is left. */
    val next: UninstallItem? get() = if (stopped) null else queue.getOrNull(results.size)
    val finished: Boolean get() = next == null
    val notAttempted: List<UninstallItem> get() = if (stopped) queue.drop(results.size) else emptyList()
    val position: Int get() = (results.size + 1).coerceAtMost(queue.size)

    fun record(outcome: UninstallOutcome): UninstallSession {
        val item = next ?: return this
        return copy(results = results + (item to outcome))
    }

    fun stop(): UninstallSession = copy(stopped = true)

    fun removed(): List<UninstallItem> = results.filter { it.second == UninstallOutcome.REMOVED }.map { it.first }
    fun cancelled(): List<UninstallItem> = results.filter { it.second == UninstallOutcome.CANCELLED }.map { it.first }
    fun failed(): List<UninstallItem> = results.filter { it.second == UninstallOutcome.FAILED }.map { it.first }
    fun stillInstalled(): List<UninstallItem> = results.filter { it.second == UninstallOutcome.STILL_INSTALLED }.map { it.first }

    companion object {
        /** Builds a session from the selected apps; ineligible ones are listed but never started. */
        fun plan(selected: List<UninstallItem>, reason: (UninstallItem) -> String?): UninstallSession {
            val queue = ArrayList<UninstallItem>()
            val skipped = ArrayList<Pair<UninstallItem, String>>()
            for (item in selected.distinctBy { it.packageName }) {
                val why = reason(item)
                if (why == null) queue.add(item) else skipped.add(item to why)
            }
            return UninstallSession(queue, skipped)
        }
    }
}
