package com.mylauncher.app.timer

import kotlin.math.max
import kotlin.math.min

/**
 * One protected app. Usage is never counted by this app: it is read from Android's own usage events
 * (see [TimerMath]) between [periodStartMs] and now, so closing, killing or restarting My Launcher or the
 * phone does not lose or reset anything.
 */
data class AppTimer(
    val packageName: String,
    val limitMinutes: Int,
    /** Usage before this moment does not count. Set when the timer is created and on every reset. */
    val periodStartMs: Long,
    /** Extra time a parent granted in this period, on top of the limit. */
    val extraMs: Long = 0L,
    /** Highest usage seen in this period. Protects against the clock being moved backwards. */
    val usedSnapshotMs: Long = 0L,
    /** When on, the allowance starts again each morning (local midnight). Otherwise only a parent resets it. */
    val renewDaily: Boolean = true,
) {
    val limitMs: Long get() = limitMinutes * 60_000L
}

object TimerRules {
    const val MIN_MINUTES = 1
    const val MAX_MINUTES = 24 * 60
    const val MAX_TIMERS = 100
    const val MAX_EXTRA_MS = 24L * 60 * 60_000L
    val PRESET_MINUTES = listOf(15, 30, 45, 60, 90, 120)
    val EXTRA_MINUTES = listOf(15, 30, 60)

    fun isValidLimit(minutes: Int): Boolean = minutes in MIN_MINUTES..MAX_MINUTES

    /** My Launcher itself can never be limited (that would lock the user out of the home screen). */
    fun canProtect(packageName: String, ownPackage: String): Boolean =
        packageName.isNotBlank() &&
            packageName != ownPackage &&
            packageName.none { it == '\t' || it == '\n' || it == '\r' }
}

enum class UsageKind { FOREGROUND, BACKGROUND, SCREEN_OFF, SHUTDOWN }

/** A trimmed-down Android usage event. [packageName] is empty for screen and shutdown events. */
data class UsageEvent(val timeMs: Long, val kind: UsageKind, val packageName: String = "")

/** Pure timer arithmetic, kept free of Android classes so it is unit tested on the JVM. */
object TimerMath {
    /** A session with no end event (for example the app was killed) is never counted for longer than this. */
    const val MAX_SESSION_MS = 12L * 60 * 60_000L

    /** Reads usage events as a small state machine: only one app is in the foreground at a time. */
    private class Replay(val pkg: String, val fromMs: Long, val toMs: Long) {
        var openSince: Long? = null
        var total = 0L

        fun close(at: Long) {
            val start = openSince ?: return
            openSince = null
            add(start, at)
        }

        fun add(start: Long, end: Long) {
            val s = max(start, fromMs)
            val e = min(min(end, toMs), start + MAX_SESSION_MS)
            if (e > s) total += e - s
        }
    }

    private fun replay(events: List<UsageEvent>, pkg: String, fromMs: Long, nowMs: Long): Replay {
        val r = Replay(pkg, fromMs, nowMs)
        for (e in events.filter { it.timeMs <= nowMs }.sortedBy { it.timeMs }) {
            when (e.kind) {
                UsageKind.FOREGROUND ->
                    if (e.packageName == pkg) {
                        if (r.openSince == null) r.openSince = e.timeMs
                    } else {
                        r.close(e.timeMs) // another app came to the front
                    }
                UsageKind.BACKGROUND -> if (e.packageName == pkg) r.close(e.timeMs)
                UsageKind.SCREEN_OFF, UsageKind.SHUTDOWN -> r.close(e.timeMs)
            }
        }
        return r
    }

    /** Milliseconds [pkg] was in the foreground between [fromMs] and [nowMs]. */
    fun foregroundMs(events: List<UsageEvent>, pkg: String, fromMs: Long, nowMs: Long): Long {
        val r = replay(events, pkg, fromMs, nowMs)
        r.openSince?.let { r.add(it, nowMs) } // still open now
        return r.total
    }

    /** True if, after the last event, [pkg] is the app on screen. */
    fun isForeground(events: List<UsageEvent>, pkg: String, nowMs: Long): Boolean =
        replay(events, pkg, Long.MIN_VALUE, nowMs).openSince != null

    data class Status(val limitMs: Long, val extraMs: Long, val usedMs: Long) {
        val allowanceMs: Long get() = limitMs + extraMs
        val remainingMs: Long get() = max(0L, allowanceMs - usedMs)
        val exhausted: Boolean get() = remainingMs <= 0L
    }

    /** Usage that counts for [timer]: never less than what was already seen, and never reset by a clock change. */
    fun usedMs(timer: AppTimer, eventsUsedMs: Long, nowMs: Long): Long =
        if (nowMs < timer.periodStartMs) timer.usedSnapshotMs else max(eventsUsedMs, timer.usedSnapshotMs)

    fun status(timer: AppTimer, eventsUsedMs: Long, nowMs: Long): Status =
        Status(timer.limitMs, timer.extraMs, usedMs(timer, eventsUsedMs, nowMs))

    /** Starts a new period at [todayStartMs] for daily timers whose period began before today. */
    fun rollIfNeeded(timer: AppTimer, nowMs: Long, todayStartMs: Long): AppTimer =
        if (timer.renewDaily && timer.periodStartMs < todayStartMs && nowMs >= todayStartMs) {
            timer.copy(periodStartMs = todayStartMs, extraMs = 0L, usedSnapshotMs = 0L)
        } else {
            timer
        }

    /** Full allowance again from now. Configuration (limit, daily setting) is kept. */
    fun reset(timer: AppTimer, nowMs: Long): AppTimer =
        timer.copy(periodStartMs = nowMs, extraMs = 0L, usedSnapshotMs = 0L)

    /** Adds [minutes] on top of the current allowance without erasing what was used. */
    fun grantExtra(timer: AppTimer, minutes: Int, status: Status): AppTimer {
        if (minutes <= 0) return timer
        // Grant relative to what is left, so "+15" always means "15 more minutes from now" even if the app ran over.
        val needed = status.usedMs - timer.limitMs + minutes * 60_000L
        return timer.copy(extraMs = min(TimerRules.MAX_EXTRA_MS, max(timer.extraMs, needed)))
    }

    /** Remembers [usedMs] only when it moved by a minute or more (fewer writes), or when time ran out. */
    fun withSnapshot(timer: AppTimer, usedMs: Long, exhausted: Boolean): AppTimer? =
        if (usedMs > timer.usedSnapshotMs && (exhausted || usedMs - timer.usedSnapshotMs >= 60_000L)) {
            timer.copy(usedSnapshotMs = usedMs)
        } else {
            null
        }
}

/** Saves the timer list as plain text lines in DataStore. Bad or hostile input is dropped, never trusted. */
object TimerCodec {
    fun encode(timers: List<AppTimer>): String = timers.joinToString("\n") {
        listOf(it.packageName, it.limitMinutes, it.periodStartMs, it.extraMs, it.usedSnapshotMs, if (it.renewDaily) 1 else 0)
            .joinToString("\t")
    }

    fun decode(raw: String?): List<AppTimer> {
        if (raw.isNullOrBlank()) return emptyList()
        val seen = LinkedHashMap<String, AppTimer>()
        for (line in raw.lineSequence()) {
            val p = line.split('\t')
            if (p.size != 6) continue
            val pkg = p[0]
            val limit = p[1].toIntOrNull()?.takeIf { TimerRules.isValidLimit(it) } ?: continue
            val start = p[2].toLongOrNull()?.takeIf { it >= 0L } ?: continue
            val extra = (p[3].toLongOrNull() ?: continue).coerceIn(0L, TimerRules.MAX_EXTRA_MS)
            val used = (p[4].toLongOrNull() ?: continue).coerceAtLeast(0L)
            if (pkg.isBlank() || seen.size >= TimerRules.MAX_TIMERS) continue
            seen.putIfAbsent(pkg, AppTimer(pkg, limit, start, extra, used, p[5] == "1"))
        }
        return seen.values.toList()
    }
}

/**
 * Slows down PIN guessing for the timer PIN. The state is saved by the caller, so closing the app does not
 * give a fresh set of attempts. The wait is capped so a changed phone clock can never lock a parent out for long.
 */
object PinGuard {
    const val FREE_ATTEMPTS = 5
    const val FIRST_BLOCK_MS = 30_000L
    const val NEXT_BLOCK_MS = 60_000L
    const val LONG_BLOCK_MS = 300_000L

    data class State(val failures: Int = 0, val blockedUntilMs: Long = 0L)

    fun remainingMs(state: State, nowMs: Long): Long = (state.blockedUntilMs - nowMs).coerceIn(0L, LONG_BLOCK_MS)

    fun onFailure(state: State, nowMs: Long): State {
        val failures = state.failures + 1
        val block = when {
            failures < FREE_ATTEMPTS -> 0L
            failures == FREE_ATTEMPTS -> FIRST_BLOCK_MS
            failures < 2 * FREE_ATTEMPTS -> NEXT_BLOCK_MS
            else -> LONG_BLOCK_MS
        }
        return State(failures, if (block == 0L) state.blockedUntilMs else nowMs + block)
    }

    fun onSuccess(): State = State()

    fun encode(s: State): String = "${s.failures},${s.blockedUntilMs}"

    fun decode(raw: String?): State {
        val p = raw?.split(',') ?: return State()
        if (p.size != 2) return State()
        val f = p[0].toIntOrNull()?.coerceIn(0, 1_000) ?: return State()
        val b = p[1].toLongOrNull()?.coerceAtLeast(0L) ?: return State()
        return State(f, b)
    }
}

/** Everything saved about timers. The PIN is stored only as a salted hash, never as text. */
data class TimerState(
    val enabled: Boolean = false,
    val timers: List<AppTimer> = emptyList(),
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometric: Boolean = false,
) {
    val pinSet: Boolean get() = !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()

    /** Timers are only enforced when switched on and a parent PIN exists to authorize more time. */
    val active: Boolean get() = enabled && pinSet && timers.isNotEmpty()

    fun timerFor(packageName: String): AppTimer? = timers.firstOrNull { it.packageName == packageName }
}
