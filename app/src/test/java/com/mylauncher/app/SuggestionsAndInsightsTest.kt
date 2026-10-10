package com.mylauncher.app

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LaunchEvent
import com.mylauncher.app.data.preferences.LaunchLogCodec
import com.mylauncher.app.insights.InsightRange
import com.mylauncher.app.insights.UsageInsights
import com.mylauncher.app.suggestions.DayPart
import com.mylauncher.app.suggestions.SmartSuggestions
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionsAndInsightsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(day: Int, hour: Int): Long = Calendar.getInstance(utc).run {
        clear(); set(2026, Calendar.MARCH, day, hour, 30, 0); timeInMillis
    }
    private fun app(label: String) = AppInfo("com.$label", "Main", label)
    private val chrome = app("Chrome"); private val youtube = app("YouTube"); private val maps = app("Maps")
    private val apps = listOf(chrome, youtube, maps)

    @Test fun dayPartBoundaries() {
        assertEquals(DayPart.NIGHT, DayPart.ofHour(4)); assertEquals(DayPart.MORNING, DayPart.ofHour(5))
        assertEquals(DayPart.AFTERNOON, DayPart.ofHour(12)); assertEquals(DayPart.EVENING, DayPart.ofHour(17))
        assertEquals(DayPart.NIGHT, DayPart.ofHour(22)); assertEquals(DayPart.NIGHT, DayPart.ofHour(0))
    }

    @Test fun timeOfDayPatternWins() {
        val events = (1..6).flatMap { d ->
            listOf(LaunchEvent("com.Chrome", at(d, 8)), LaunchEvent("com.YouTube", at(d, 20)))
        }
        val morning = SmartSuggestions.suggest(events, emptySet(), apps, at(7, 8), utc)
        val evening = SmartSuggestions.suggest(events, emptySet(), apps, at(7, 20), utc)
        assertEquals("Chrome", morning.first().label)
        assertEquals("YouTube", evening.first().label)
    }

    @Test fun favoritesFillWhenThereIsNoHistoryAndNothingWithoutEither() {
        assertTrue(SmartSuggestions.suggest(emptyList(), emptySet(), apps, at(7, 8), utc).isEmpty())
        assertEquals(listOf("Maps"), SmartSuggestions.suggest(emptyList(), setOf("com.Maps"), apps, at(7, 8), utc).map { it.label })
    }

    @Test fun uninstalledAppsAreNeverSuggested() {
        val events = listOf(LaunchEvent("com.Gone", at(6, 8)))
        assertTrue(SmartSuggestions.suggest(events, emptySet(), apps, at(7, 8), utc).isEmpty())
    }

    @Test fun insightsCountTodayAndWeek() {
        val now = at(10, 15)
        val events = listOf(
            LaunchEvent("com.Chrome", at(10, 9)), LaunchEvent("com.Chrome", at(10, 11)),
            LaunchEvent("com.YouTube", at(10, 12)),
            LaunchEvent("com.Maps", at(5, 12)),   // 5 days ago: in week, not today
            LaunchEvent("com.Maps", at(2, 12)),   // too old for week
        )
        val today = UsageInsights.compute(events, apps, InsightRange.TODAY, now, utc)
        assertEquals(3, today.totalLaunches)
        assertEquals(listOf("Chrome" to 2, "YouTube" to 1), today.mostUsed.map { it.app.label to it.launches })
        assertEquals("YouTube", today.recentlyOpened.first().label)
        val week = UsageInsights.compute(events, apps, InsightRange.WEEK, now, utc)
        assertEquals(4, week.totalLaunches)
        assertTrue(week.mostUsed.any { it.app.label == "Maps" && it.launches == 1 })
    }

    @Test fun launchLogCodecIsCappedAndSkipsBadLines() {
        var log = emptyList<LaunchEvent>()
        repeat(LaunchLogCodec.MAX_EVENTS + 25) { log = LaunchLogCodec.append(log, "p$it", 1000L + it) }
        assertEquals(LaunchLogCodec.MAX_EVENTS, log.size)
        assertEquals(log, LaunchLogCodec.decode(LaunchLogCodec.encode(log)))
        assertEquals(1, LaunchLogCodec.decode("bad\n\tx\npkg\t123\npkg\tabc").size)
    }
}
