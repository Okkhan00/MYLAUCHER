package com.mylauncher.app.data.preferences

import com.mylauncher.app.data.model.LaunchStat

/** Launch history lives only on this device. One app per line: package TAB count TAB lastLaunchedMs. */
object LaunchStatsCodec {
    const val MAX_ENTRIES = 200

    fun encode(stats: Map<String, LaunchStat>): String =
        stats.entries
            .sortedByDescending { it.value.lastLaunchedMs }
            .take(MAX_ENTRIES)
            .joinToString("\n") { "${it.key}\t${it.value.count}\t${it.value.lastLaunchedMs}" }

    fun decode(raw: String?): Map<String, LaunchStat> {
        val out = LinkedHashMap<String, LaunchStat>()
        raw?.lineSequence()?.forEach { line ->
            val parts = line.split("\t")
            if (parts.size == 3 && parts[0].isNotBlank()) {
                val count = parts[1].toIntOrNull()
                val last = parts[2].toLongOrNull()
                if (count != null && last != null && count > 0) out[parts[0]] = LaunchStat(count, last)
            }
        }
        return out
    }

    fun record(stats: Map<String, LaunchStat>, pkg: String, nowMs: Long): Map<String, LaunchStat> {
        val old = stats[pkg]
        return stats + (pkg to LaunchStat((old?.count ?: 0) + 1, nowMs))
    }
}
