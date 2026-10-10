package com.mylauncher.app.timer

import android.content.Context
import com.mylauncher.app.data.preferences.LauncherPreferences
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Connects the pure timer rules to Android: reads usage events, saves the few values that change, and
 * schedules at most one quiet alarm per limited app. Nothing here runs on a timer or in a service.
 */
class TimerEngine(context: Context, private val prefs: LauncherPreferences) {
    private val appContext = context.applicationContext

    data class Evaluation(
        val timer: AppTimer,
        val status: TimerMath.Status,
        /** The app is on screen right now. */
        val foreground: Boolean,
        /** False when Usage Access is off: time cannot be measured, so nothing is enforced. */
        val usageAvailable: Boolean,
    )

    sealed interface Gate {
        /** No active timer for this app (or timers cannot be measured): open it normally. */
        data object Open : Gate
        data class Blocked(val evaluation: Evaluation) : Gate
    }

    fun startOfToday(nowMs: Long): Long = Calendar.getInstance().run {
        timeInMillis = nowMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    /** Reads usage off the main thread, applies the daily renewal and saves only what changed. */
    suspend fun evaluate(timer: AppTimer, nowMs: Long = System.currentTimeMillis()): Evaluation = withContext(Dispatchers.IO) {
        val rolled = TimerMath.rollIfNeeded(timer, nowMs, startOfToday(nowMs))
        val granted = UsageAccess.isGranted(appContext)
        if (!granted) {
            persistIfChanged(timer, rolled)
            return@withContext Evaluation(rolled, TimerMath.status(rolled, 0L, nowMs), foreground = false, usageAvailable = false)
        }
        // Android keeps fine-grained events for about a week; a limit that is not renewed daily falls back on the snapshot.
        val from = maxOf(rolled.periodStartMs - TimerMath.MAX_SESSION_MS, nowMs - EVENT_WINDOW_MS)
        val events = UsageAccess.readEvents(appContext, from, nowMs)
        val used = TimerMath.foregroundMs(events, rolled.packageName, rolled.periodStartMs, nowMs)
        val status = TimerMath.status(rolled, used, nowMs)
        val final = TimerMath.withSnapshot(rolled, status.usedMs, status.exhausted) ?: rolled
        persistIfChanged(timer, final)
        Evaluation(final, status, TimerMath.isForeground(events, rolled.packageName, nowMs), usageAvailable = true)
    }

    private suspend fun persistIfChanged(original: AppTimer, updated: AppTimer) {
        if (updated != original) prefs.updateTimerIfSamePeriod(updated, original.periodStartMs, original.extraMs)
    }

    /** Called when an app is about to be opened from the launcher. */
    suspend fun gate(packageName: String): Gate {
        val state = prefs.timerState.first()
        val timer = state.timerFor(packageName)
        if (!state.active || timer == null) return Gate.Open
        val e = evaluate(timer)
        return if (e.usageAvailable && e.status.exhausted) Gate.Blocked(e) else Gate.Open
    }

    /** Schedules the "time is up" reminder for when the remaining time would run out if the app stays open. */
    suspend fun armFor(packageName: String) {
        val state = prefs.timerState.first()
        val timer = state.timerFor(packageName)
        if (!state.active || timer == null) return
        val e = evaluate(timer)
        if (e.usageAvailable && !e.status.exhausted) {
            TimerAlarm.schedule(appContext, packageName, System.currentTimeMillis() + e.status.remainingMs + ALARM_SLACK_MS)
        }
    }

    /** The alarm fired: remind if time is up, otherwise try again later only while the app is still open. */
    suspend fun onAlarm(packageName: String) {
        val state = prefs.timerState.first()
        val timer = state.timerFor(packageName)
        if (!state.active || timer == null) return
        val e = evaluate(timer)
        if (!e.usageAvailable) return
        if (e.status.exhausted) {
            TimerNotifier.postTimeUp(appContext, packageName)
        } else if (e.foreground) {
            TimerAlarm.schedule(appContext, packageName, System.currentTimeMillis() + e.status.remainingMs + ALARM_SLACK_MS)
        }
    }

    suspend fun evaluateAll(): Map<String, Evaluation> {
        val timers = prefs.timerState.first().timers
        return timers.associate { it.packageName to evaluate(it) }
    }

    /** Full allowance again. Returns false if the app no longer has a timer. */
    suspend fun reset(packageName: String): Boolean {
        var found = false
        val now = System.currentTimeMillis()
        prefs.updateTimers { list ->
            list.map {
                if (it.packageName == packageName) {
                    found = true
                    TimerMath.reset(it, now)
                } else it
            }
        }
        TimerAlarm.cancel(appContext, packageName)
        TimerNotifier.cancel(appContext, packageName)
        return found
    }

    /** Adds [minutes] on top of what is left. */
    suspend fun grantExtra(packageName: String, minutes: Int): Boolean {
        val timer = prefs.timerState.first().timerFor(packageName) ?: return false
        val e = evaluate(timer)
        prefs.updateTimers { list ->
            list.map { if (it.packageName == packageName) TimerMath.grantExtra(it, minutes, e.status.copy(extraMs = it.extraMs)) else it }
        }
        TimerNotifier.cancel(appContext, packageName)
        return true
    }

    private companion object {
        const val EVENT_WINDOW_MS = 7L * 24 * 60 * 60_000L
        const val ALARM_SLACK_MS = 2_000L
    }
}
