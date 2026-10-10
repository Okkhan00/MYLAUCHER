package com.mylauncher.app.timer

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Usage Access is a special permission: Android does not show a normal permission dialog, the user has to
 * switch it on in system settings. It lets this app read how long other apps were on screen. The data is
 * only used on this device for the timers the user sets up.
 */
object UsageAccess {
    fun isGranted(context: Context): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        val mode = if (ops == null) {
            AppOpsManager.MODE_IGNORED
        } else if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: Exception) {
        false
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    // Android's event type numbers, kept literal because some names are deprecated or newer than minSdk 26.
    private const val ACTIVITY_RESUMED = 1 // MOVE_TO_FOREGROUND before API 29
    private const val ACTIVITY_PAUSED = 2 // MOVE_TO_BACKGROUND before API 29
    private const val SCREEN_NON_INTERACTIVE = 16
    private const val ACTIVITY_STOPPED = 23
    private const val DEVICE_SHUTDOWN = 26

    /** Foreground/background/screen events between the two times. Empty if access is off or Android refuses. */
    fun readEvents(context: Context, fromMs: Long, toMs: Long): List<UsageEvent> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        return try {
            val events = manager.queryEvents(fromMs, toMs)
            val reusable = UsageEvents.Event()
            val out = ArrayList<UsageEvent>()
            while (events.hasNextEvent()) {
                events.getNextEvent(reusable)
                val kind = when (reusable.eventType) {
                    ACTIVITY_RESUMED -> UsageKind.FOREGROUND
                    ACTIVITY_PAUSED, ACTIVITY_STOPPED -> UsageKind.BACKGROUND
                    SCREEN_NON_INTERACTIVE -> UsageKind.SCREEN_OFF
                    DEVICE_SHUTDOWN -> UsageKind.SHUTDOWN
                    else -> continue
                }
                out.add(UsageEvent(reusable.timeStamp, kind, reusable.packageName.orEmpty()))
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }
}
