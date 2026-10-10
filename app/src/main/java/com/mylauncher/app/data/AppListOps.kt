package com.mylauncher.app.data

import com.mylauncher.app.data.model.AppInfo

/**
 * Pure helpers for keeping the installed-app list up to date without re-reading everything.
 * Kept free of Android classes so they are unit tested on the JVM.
 */
object AppListOps {
    /** The one ordering used everywhere: label A-Z ignoring case, package name as a stable tie-break. */
    val ORDER: Comparator<AppInfo> =
        compareBy<AppInfo, String>(String.CASE_INSENSITIVE_ORDER) { it.label }.thenBy { it.packageName }

    /** Sorted, one entry per package. */
    fun normalize(apps: List<AppInfo>): List<AppInfo> =
        apps.distinctBy { it.packageName }.sortedWith(ORDER)

    /**
     * Replaces everything known about [packageName] with [fresh] (empty = the app is gone or no longer
     * launchable). Returns [current] itself when nothing changed, so callers can skip downstream work.
     */
    fun replacePackage(current: List<AppInfo>, packageName: String, fresh: List<AppInfo>): List<AppInfo> {
        val old = current.filter { it.packageName == packageName }
        val newEntries = fresh.filter { it.packageName == packageName }.distinctBy { it.packageName }
        if (old == newEntries) return current
        return (current.filterNot { it.packageName == packageName } + newEntries).sortedWith(ORDER)
    }
}

/**
 * Decides what a package broadcast should trigger. Pure so it can be unit tested without Android.
 */
object PackageEvents {
    enum class Plan {
        /** Nothing to do (for example the "removed" half of an app update, which is followed by "added"). */
        IGNORE,

        /** Re-check just this package. */
        REFRESH,

        /** Re-check now, and once more shortly after: Android may not have finished registering a new app. */
        REFRESH_AND_VERIFY,
    }

    const val ADDED = "android.intent.action.PACKAGE_ADDED"
    const val REMOVED = "android.intent.action.PACKAGE_REMOVED"
    const val FULLY_REMOVED = "android.intent.action.PACKAGE_FULLY_REMOVED"
    const val CHANGED = "android.intent.action.PACKAGE_CHANGED"
    const val REPLACED = "android.intent.action.PACKAGE_REPLACED"

    fun plan(action: String?, replacing: Boolean): Plan = when (action) {
        ADDED, REPLACED -> Plan.REFRESH_AND_VERIFY
        REMOVED -> if (replacing) Plan.IGNORE else Plan.REFRESH
        FULLY_REMOVED, CHANGED -> Plan.REFRESH
        else -> Plan.IGNORE
    }
}

/**
 * Tiny text format for the on-disk copy of the app list, used to show apps instantly at startup
 * while the real PackageManager query runs. A bad or old file simply decodes to an empty list.
 */
object AppListCodec {
    private const val HEADER = "mylauncher-apps-v1"

    fun encode(apps: List<AppInfo>): String = buildString {
        append(HEADER).append('\n')
        for (a in apps) {
            append(clean(a.packageName)).append('\t')
                .append(clean(a.activityName)).append('\t')
                .append(clean(a.label)).append('\t')
                .append(a.systemCategory).append('\n')
        }
    }

    fun decode(text: String?): List<AppInfo> {
        if (text.isNullOrEmpty()) return emptyList()
        val lines = text.split('\n')
        if (lines.firstOrNull() != HEADER) return emptyList()
        val out = ArrayList<AppInfo>(lines.size)
        for (line in lines.drop(1)) {
            if (line.isEmpty()) continue
            val f = line.split('\t')
            if (f.size != 4) return emptyList() // damaged file: ignore it entirely
            val category = f[3].toIntOrNull() ?: return emptyList()
            if (f[0].isBlank() || f[1].isBlank()) return emptyList()
            out.add(AppInfo(packageName = f[0], activityName = f[1], label = f[2].ifEmpty { f[0] }, systemCategory = category))
        }
        return out
    }

    private fun clean(s: String): String = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
}
