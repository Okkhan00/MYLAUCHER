package com.mylauncher.app.insights

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LaunchEvent
import java.util.Calendar
import java.util.TimeZone

enum class InsightRange(val label: String) { TODAY("Today"), WEEK("This week") }

data class UsageEntry(val app: AppInfo, val launches: Int)

data class Insights(
    val range: InsightRange,
    val mostUsed: List<UsageEntry>,
    val recentlyOpened: List<AppInfo>,
    val totalLaunches: Int,
)

/**
 * Insights from the launcher's own launch log (apps opened from My Launcher). This needs no special
 * permission. It does not see apps opened from notifications, recents or other launchers.
 */
object UsageInsights {
    /** Local midnight of the day containing [nowMs]. */
    fun startOfDay(nowMs: Long, zone: TimeZone): Long {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = nowMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Start of the window: today's midnight, or midnight six days earlier for the week view. */
    fun windowStart(range: InsightRange, nowMs: Long, zone: TimeZone): Long {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = startOfDay(nowMs, zone) }
        if (range == InsightRange.WEEK) cal.add(Calendar.DAY_OF_YEAR, -6)
        return cal.timeInMillis
    }

    fun compute(
        events: List<LaunchEvent>,
        apps: List<AppInfo>,
        range: InsightRange,
        nowMs: Long,
        zone: TimeZone = TimeZone.getDefault(),
        limit: Int = 5,
    ): Insights {
        val start = windowStart(range, nowMs, zone)
        val byPackage = apps.associateBy { it.packageName }
        val inWindow = events.filter { it.timeMs in start..nowMs && it.packageName in byPackage }

        val counts = inWindow.groupingBy { it.packageName }.eachCount()
        val last = HashMap<String, Long>()
        inWindow.forEach { last[it.packageName] = maxOf(last[it.packageName] ?: 0L, it.timeMs) }

        val mostUsed = counts.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { last[it.key] ?: 0L }
                    .thenBy { byPackage[it.key]?.label?.lowercase().orEmpty() },
            )
            .take(limit)
            .map { UsageEntry(byPackage.getValue(it.key), it.value) }

        val recent = last.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { byPackage.getValue(it.key) }

        return Insights(range, mostUsed, recent, inWindow.size)
    }
}
