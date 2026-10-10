package com.mylauncher.app

import com.mylauncher.app.timer.AppTimer
import com.mylauncher.app.timer.PinGuard
import com.mylauncher.app.timer.TimerCodec
import com.mylauncher.app.timer.TimerMath
import com.mylauncher.app.timer.TimerRules
import com.mylauncher.app.timer.UsageEvent
import com.mylauncher.app.timer.UsageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTest {
    private val min = 60_000L
    private val tt = "com.tiktok"
    private fun fg(t: Long, p: String = tt) = UsageEvent(t * min, UsageKind.FOREGROUND, p)
    private fun bg(t: Long, p: String = tt) = UsageEvent(t * min, UsageKind.BACKGROUND, p)
    private fun timer(limit: Int = 60, start: Long = 0L) = AppTimer(tt, limit, start * min)

    // ---- foreground usage ---------------------------------------------------------------

    @Test fun countsOnlyTimeInTheForeground() {
        val e = listOf(fg(0), bg(10), fg(30), bg(35))
        assertEquals(15 * min, TimerMath.foregroundMs(e, tt, 0, 100 * min))
    }

    @Test fun openSessionCountsUpToNow() {
        assertEquals(7 * min, TimerMath.foregroundMs(listOf(fg(3)), tt, 0, 10 * min))
    }

    @Test fun backgroundTimeDoesNotConsumeTheAllowance() {
        val e = listOf(fg(0), bg(5)) // left after 5 minutes, hours pass
        assertEquals(5 * min, TimerMath.foregroundMs(e, tt, 0, 600 * min))
    }

    @Test fun switchingToAnotherAppEndsTheSession() {
        // No explicit background event for TikTok: another app coming to the front ends it.
        val e = listOf(fg(0), fg(8, "com.other"), fg(20), bg(22))
        assertEquals(10 * min, TimerMath.foregroundMs(e, tt, 0, 60 * min))
    }

    @Test fun screenOffAndShutdownEndTheSession() {
        val off = listOf(fg(0), UsageEvent(4 * min, UsageKind.SCREEN_OFF))
        assertEquals(4 * min, TimerMath.foregroundMs(off, tt, 0, 120 * min))
        val down = listOf(fg(0), UsageEvent(9 * min, UsageKind.SHUTDOWN), fg(500))
        assertEquals(9 * min + (600 - 500) * min, TimerMath.foregroundMs(down, tt, 0, 600 * min))
    }

    @Test fun otherAppsUsageIsIgnored() {
        val e = listOf(fg(0, "com.other"), bg(50, "com.other"))
        assertEquals(0L, TimerMath.foregroundMs(e, tt, 0, 100 * min))
    }

    @Test fun onlyTimeAfterTheResetCounts() {
        val e = listOf(fg(0), bg(30), fg(50), bg(60))
        assertEquals(10 * min, TimerMath.foregroundMs(e, tt, 40 * min, 100 * min))
        // A session that was already running when the period started counts from the period start.
        assertEquals(20 * min, TimerMath.foregroundMs(listOf(fg(0), bg(30)), tt, 10 * min, 100 * min))
    }

    @Test fun aSessionWithNoEndIsCappedInsteadOfCountingForever() {
        val used = TimerMath.foregroundMs(listOf(fg(0)), tt, 0, 100L * 60 * min)
        assertEquals(TimerMath.MAX_SESSION_MS, used)
    }

    @Test fun eventsInTheFutureAreIgnoredAndOrderDoesNotMatter() {
        val e = listOf(bg(10), fg(0), fg(500))
        assertEquals(10 * min, TimerMath.foregroundMs(e, tt, 0, 20 * min))
    }

    @Test fun detectsWhetherTheAppIsOnScreen() {
        assertTrue(TimerMath.isForeground(listOf(fg(0)), tt, 10 * min))
        assertFalse(TimerMath.isForeground(listOf(fg(0), bg(5)), tt, 10 * min))
        assertFalse(TimerMath.isForeground(listOf(fg(0), fg(2, "x")), tt, 10 * min))
        assertFalse(TimerMath.isForeground(emptyList(), tt, 10 * min))
    }

    // ---- allowance ------------------------------------------------------------------------

    @Test fun remainingTimeAndExpiry() {
        val s = TimerMath.status(timer(60), 45 * min, 50 * min)
        assertEquals(15 * min, s.remainingMs)
        assertFalse(s.exhausted)
        assertTrue(TimerMath.status(timer(60), 60 * min, 70 * min).exhausted)
        assertTrue(TimerMath.status(timer(60), 75 * min, 80 * min).exhausted)
    }

    @Test fun movingTheClockBackCannotGiveTimeBack() {
        val t = timer(60, start = 100).copy(usedSnapshotMs = 60 * min)
        // "Now" is before the period start (clock moved back): the snapshot is used, usage does not drop to 0.
        assertTrue(TimerMath.status(t, 0, 50 * min).exhausted)
        // Events read for a smaller window can never lower the used time either.
        assertTrue(TimerMath.status(t, 5 * min, 120 * min).exhausted)
    }

    @Test fun snapshotIsOnlyWrittenWhenUseful() {
        val t = timer()
        assertEquals(null, TimerMath.withSnapshot(t, 30_000L, exhausted = false))
        assertEquals(5 * min, TimerMath.withSnapshot(t, 5 * min, exhausted = false)!!.usedSnapshotMs)
        assertEquals(30_000L, TimerMath.withSnapshot(t, 30_000L, exhausted = true)!!.usedSnapshotMs)
        assertEquals(null, TimerMath.withSnapshot(t.copy(usedSnapshotMs = 5 * min), 5 * min, exhausted = true))
    }

    // ---- reset and extra time -----------------------------------------------------------------

    @Test fun resetGivesAFullAllowanceAndKeepsTheConfiguration() {
        val used = timer(30).copy(extraMs = 10 * min, usedSnapshotMs = 40 * min, renewDaily = false)
        val r = TimerMath.reset(used, 500 * min)
        assertEquals(500 * min, r.periodStartMs)
        assertEquals(0L, r.extraMs)
        assertEquals(0L, r.usedSnapshotMs)
        assertEquals(30, r.limitMinutes)
        assertFalse(r.renewDaily)
        assertEquals(tt, r.packageName)
        // Right after a reset the same events no longer count.
        val e = listOf(fg(0), bg(40))
        assertEquals(0L, TimerMath.foregroundMs(e, tt, r.periodStartMs, 501 * min))
        assertFalse(TimerMath.status(r, 0, 501 * min).exhausted)
    }

    @Test fun grantedExtraTimeIsMeasuredFromNow() {
        val t = timer(60).copy(usedSnapshotMs = 70 * min) // ran 10 minutes over
        val s = TimerMath.status(t, 70 * min, 100 * min)
        assertTrue(s.exhausted)
        val granted = TimerMath.grantExtra(t, 15, s)
        val after = TimerMath.status(granted, 70 * min, 100 * min)
        assertEquals(15 * min, after.remainingMs)
    }

    @Test fun extraTimeIsCappedAndIgnoresNonsense() {
        val t = timer(60)
        val s = TimerMath.status(t, 60 * min, 70 * min)
        assertEquals(t, TimerMath.grantExtra(t, 0, s))
        assertEquals(t, TimerMath.grantExtra(t, -5, s))
        val huge = TimerMath.grantExtra(t, 1_000_000, s)
        assertEquals(TimerRules.MAX_EXTRA_MS, huge.extraMs)
    }

    @Test fun repeatedGrantsDoNotStackBeyondWhatWasAsked() {
        val t = timer(60).copy(usedSnapshotMs = 60 * min)
        val s = TimerMath.status(t, 60 * min, 100 * min)
        val once = TimerMath.grantExtra(t, 15, s)
        assertEquals(15 * min, once.extraMs)
        // Granting again without using any time still leaves 15 minutes, not 30.
        val twice = TimerMath.grantExtra(once, 15, TimerMath.status(once, 60 * min, 100 * min))
        assertEquals(15 * min, TimerMath.status(twice, 60 * min, 100 * min).remainingMs)
    }

    // ---- daily renewal ---------------------------------------------------------------------------

    @Test fun dailyTimerStartsANewPeriodEachMorning() {
        val yesterday = timer(60, start = 100).copy(extraMs = 5 * min, usedSnapshotMs = 60 * min)
        val rolled = TimerMath.rollIfNeeded(yesterday, 1500 * min, 1440 * min)
        assertEquals(1440 * min, rolled.periodStartMs)
        assertEquals(0L, rolled.usedSnapshotMs)
        assertEquals(0L, rolled.extraMs)
        // Already today: unchanged. Not daily: unchanged. Clock before today's start: unchanged.
        assertEquals(rolled, TimerMath.rollIfNeeded(rolled, 1600 * min, 1440 * min))
        val manual = yesterday.copy(renewDaily = false)
        assertEquals(manual, TimerMath.rollIfNeeded(manual, 1500 * min, 1440 * min))
        assertEquals(yesterday, TimerMath.rollIfNeeded(yesterday, 1400 * min, 1440 * min))
    }

    // ---- selection rules and storage ------------------------------------------------------------

    @Test fun myLauncherCannotBeProtected() {
        assertFalse(TimerRules.canProtect("com.mylauncher.app", "com.mylauncher.app"))
        assertFalse(TimerRules.canProtect("", "com.mylauncher.app"))
        assertFalse(TimerRules.canProtect("bad\tname", "com.mylauncher.app"))
        assertTrue(TimerRules.canProtect(tt, "com.mylauncher.app"))
    }

    @Test fun limitsAreRangeChecked() {
        assertFalse(TimerRules.isValidLimit(0))
        assertTrue(TimerRules.isValidLimit(1))
        assertTrue(TimerRules.isValidLimit(1440))
        assertFalse(TimerRules.isValidLimit(1441))
        assertTrue(TimerRules.PRESET_MINUTES.containsAll(listOf(15, 30, 45, 60, 90, 120)))
    }

    @Test fun codecRoundTripKeepsEveryField() {
        val list = listOf(
            AppTimer(tt, 60, 1_000L, 5 * min, 20 * min, true),
            AppTimer("com.game", 30, 2_000L, 0L, 0L, false),
        )
        assertEquals(list, TimerCodec.decode(TimerCodec.encode(list)))
    }

    @Test fun codecRejectsBadLinesAndDuplicates() {
        val raw = listOf(
            "a.b\t60\t1\t0\t0\t1",
            "a.b\t30\t1\t0\t0\t1", // duplicate package
            "c.d\t0\t1\t0\t0\t1", // limit too small
            "e.f\tx\t1\t0\t0\t1", // not a number
            "g.h\t10\t-5\t0\t0\t1", // negative start
            "too\tfew",
            "i.j\t10\t5\t999999999999\t-3\t0", // values clamped
        ).joinToString("\n")
        val out = TimerCodec.decode(raw)
        assertEquals(listOf("a.b", "i.j"), out.map { it.packageName })
        assertEquals(60, out.first().limitMinutes)
        assertEquals(TimerRules.MAX_EXTRA_MS, out.last().extraMs)
        assertEquals(0L, out.last().usedSnapshotMs)
        assertTrue(TimerCodec.decode(null).isEmpty())
        assertTrue(TimerCodec.decode("").isEmpty())
    }

    // ---- PIN guessing protection ------------------------------------------------------------------

    @Test fun pinGuardBlocksAfterFiveWrongTriesAndSurvivesRestart() {
        var s = PinGuard.State()
        for (i in 1..4) {
            s = PinGuard.onFailure(s, 1_000L)
            assertEquals(0L, PinGuard.remainingMs(s, 1_000L))
        }
        s = PinGuard.onFailure(s, 1_000L)
        assertEquals(PinGuard.FIRST_BLOCK_MS, PinGuard.remainingMs(s, 1_000L))
        // Saved and loaded again (app restart): still blocked.
        val reloaded = PinGuard.decode(PinGuard.encode(s))
        assertEquals(s, reloaded)
        assertEquals(PinGuard.FIRST_BLOCK_MS - 10_000L, PinGuard.remainingMs(reloaded, 11_000L))
        assertEquals(0L, PinGuard.remainingMs(reloaded, 1_000L + PinGuard.FIRST_BLOCK_MS))
    }

    @Test fun pinGuardGetsStricterAndResetsOnSuccess() {
        var s = PinGuard.State()
        repeat(6) { s = PinGuard.onFailure(s, 0L) }
        assertEquals(PinGuard.NEXT_BLOCK_MS, PinGuard.remainingMs(s, 0L))
        repeat(4) { s = PinGuard.onFailure(s, 0L) }
        assertEquals(PinGuard.LONG_BLOCK_MS, PinGuard.remainingMs(s, 0L))
        assertEquals(PinGuard.State(), PinGuard.onSuccess())
    }

    @Test fun pinGuardWaitIsCappedIfTheClockJumpedBack() {
        val s = PinGuard.State(7, blockedUntilMs = 10_000_000_000L)
        assertEquals(PinGuard.LONG_BLOCK_MS, PinGuard.remainingMs(s, 0L))
    }

    @Test fun pinGuardDecodeIgnoresGarbage() {
        assertEquals(PinGuard.State(), PinGuard.decode(null))
        assertEquals(PinGuard.State(), PinGuard.decode("nope"))
        assertEquals(PinGuard.State(), PinGuard.decode("a,b"))
    }
}
