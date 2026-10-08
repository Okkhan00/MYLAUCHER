package com.mylauncher.app.suggestions

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LaunchEvent
import java.util.Calendar
import java.util.TimeZone

enum class DayPart(val label: String) {
    MORNING("Morning"), AFTERNOON("Afternoon"), EVENING("Evening"), NIGHT("Night");

    companion object {
        fun ofHour(hour: Int): DayPart = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }

        fun of(timeMs: Long, zone: TimeZone): DayPart {
            val cal = Calendar.getInstance(zone).apply { timeInMillis = timeMs }
            return ofHour(cal.get(Calendar.HOUR_OF_DAY))
        }
    }
}

/**
 * On-device suggestions. Inputs are only the local launch log and the user's favorites; nothing is
 * sent anywhere. Score = recent launches (older ones decay with a 14-day half-life), weighted three
 * times higher when they happened in the same part of the day as now, plus a small bonus for favorites.
 */
object SmartSuggestions {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val HALF_LIFE_DAYS = 14.0
    private const val SAME_PART_WEIGHT = 3.0
    private const val FAVORITE_BONUS = 0.5

    fun suggest(
        events: List<LaunchEvent>,
        favorites: Set<String>,
        apps: List<AppInfo>,
        nowMs: Long,
        zone: TimeZone = TimeZone.getDefault(),
        limit: Int = 4,
    ): List<AppInfo> {
        if (limit <= 0) return emptyList()
        val byPackage = apps.associateBy { it.packageName }
        val currentPart = DayPart.of(nowMs, zone)
        val scores = HashMap<String, Double>()
        val lastUsed = HashMap<String, Long>()

        for (e in events) {
            if (e.packageName !in byPackage || e.timeMs > nowMs) continue
            val ageDays = (nowMs - e.timeMs).toDouble() / DAY_MS
            val decay = Math.pow(0.5, ageDays / HALF_LIFE_DAYS)
            val weight = if (DayPart.of(e.timeMs, zone) == currentPart) SAME_PART_WEIGHT else 1.0
            scores[e.packageName] = (scores[e.packageName] ?: 0.0) + decay * weight
            lastUsed[e.packageName] = maxOf(lastUsed[e.packageName] ?: 0L, e.timeMs)
        }
        for (pkg in favorites) {
            if (pkg in byPackage) scores[pkg] = (scores[pkg] ?: 0.0) + FAVORITE_BONUS
        }

        return scores.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, Double>> { it.value }
                    .thenByDescending { lastUsed[it.key] ?: 0L }
                    .thenBy { byPackage[it.key]?.label?.lowercase() ?: it.key },
            )
            .mapNotNull { byPackage[it.key] }
            .take(limit)
    }
}
