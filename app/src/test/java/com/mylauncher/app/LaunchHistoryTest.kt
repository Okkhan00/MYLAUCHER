package com.mylauncher.app

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.data.preferences.LaunchStatsCodec
import com.mylauncher.app.launcher.apps.LaunchHistory
import com.mylauncher.app.launcher.home.BatteryMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchHistoryTest {
    private fun app(pkg: String) = AppInfo(pkg, "$pkg.Main", pkg)
    private val apps = listOf(app("a"), app("b"), app("c"), app("d"))

    @Test fun recordCountsAndTimestamps() {
        var stats = LaunchStatsCodec.record(emptyMap(), "a", 100L)
        stats = LaunchStatsCodec.record(stats, "a", 200L)
        stats = LaunchStatsCodec.record(stats, "b", 300L)
        assertEquals(LaunchStat(2, 200L), stats["a"])
        assertEquals(LaunchStat(1, 300L), stats["b"])
    }

    @Test fun codecRoundTripsAndIgnoresBadLines() {
        val stats = mapOf("a" to LaunchStat(3, 10L), "b" to LaunchStat(1, 20L))
        assertEquals(stats, LaunchStatsCodec.decode(LaunchStatsCodec.encode(stats)))
        val decoded = LaunchStatsCodec.decode("bad\na\t2\t5\nb\tx\t5\nc\t0\t5\n\t1\t1")
        assertEquals(setOf("a"), decoded.keys)
    }

    @Test fun encodeKeepsOnlyTheNewestEntries() {
        val many = (1..LaunchStatsCodec.MAX_ENTRIES + 20).associate { "p$it" to LaunchStat(1, it.toLong()) }
        val decoded = LaunchStatsCodec.decode(LaunchStatsCodec.encode(many))
        assertEquals(LaunchStatsCodec.MAX_ENTRIES, decoded.size)
        assertNull(decoded["p1"])
    }

    @Test fun recentIsNewestFirstAndSkipsUninstalledApps() {
        val stats = mapOf("a" to LaunchStat(1, 10L), "b" to LaunchStat(1, 30L), "gone" to LaunchStat(1, 99L), "c" to LaunchStat(1, 20L))
        assertEquals(listOf("b", "c", "a"), LaunchHistory.recent(stats, apps).map { it.packageName })
        assertEquals(listOf("b"), LaunchHistory.recent(stats, apps, limit = 1).map { it.packageName })
    }

    @Test fun mostUsedNeedsMinimumCountAndSortsByCount() {
        val stats = mapOf("a" to LaunchStat(2, 10L), "b" to LaunchStat(9, 5L), "c" to LaunchStat(5, 1L), "d" to LaunchStat(5, 2L))
        assertEquals(listOf("b", "d", "c"), LaunchHistory.mostUsed(stats, apps).map { it.packageName })
    }

    @Test fun batteryPercentHandlesBadValues() {
        assertEquals(82, BatteryMath.percentOf(82, 100))
        assertEquals(50, BatteryMath.percentOf(127, 254))
        assertEquals(100, BatteryMath.percentOf(150, 100))
        assertNull(BatteryMath.percentOf(-1, 100))
        assertNull(BatteryMath.percentOf(50, 0))
    }
}
