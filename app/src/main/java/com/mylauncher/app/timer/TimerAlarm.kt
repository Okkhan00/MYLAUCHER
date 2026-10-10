package com.mylauncher.app.timer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mylauncher.app.MainActivity
import com.mylauncher.app.MyLauncherApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * One inexact alarm per limited app, set only when such an app is opened from My Launcher. It needs no
 * special alarm permission, may fire a little late (Android batches alarms), is lost if the phone restarts,
 * and does nothing except re-check the time and post a reminder.
 */
object TimerAlarm {
    const val EXTRA_PACKAGE = "timer_package"

    private fun pending(context: Context, packageName: String, flags: Int): PendingIntent? {
        val intent = Intent(context, TimerAlarmReceiver::class.java).putExtra(EXTRA_PACKAGE, packageName)
        // The action makes the intent unique per app, so each limited app has its own alarm.
        intent.action = "com.mylauncher.app.TIMER_ALARM.$packageName"
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(context: Context, packageName: String, atMs: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val p = pending(context, packageName, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        try {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, p)
        } catch (e: Exception) {
            // Without an alarm the launcher still checks the time whenever the app is opened from it.
        }
    }

    fun cancel(context: Context, packageName: String) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        pending(context, packageName, PendingIntent.FLAG_NO_CREATE)?.let { manager.cancel(it) }
    }
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(TimerAlarm.EXTRA_PACKAGE) ?: return
        val app = context.applicationContext as? MyLauncherApplication ?: return
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                app.container.timerEngine.onAlarm(packageName)
            } catch (e: Exception) {
                // A failed check is harmless; the next launch from the launcher checks again.
            } finally {
                result.finish()
            }
        }
    }
}

/** The calm "time is up" reminder. Tapping it opens My Launcher's parent verification, not the limited app. */
object TimerNotifier {
    private const val CHANNEL_ID = "app_timer"

    private fun notificationId(packageName: String) = 4000 + (packageName.hashCode() and 0xFFFF)

    private fun label(context: Context, packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (e: Exception) {
        packageName
    }

    fun postTimeUp(context: Context, packageName: String) {
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "App usage timer", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "A reminder when the allowed time for an app has ended"
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(TimerAlarm.EXTRA_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val content = PendingIntent.getActivity(
            context, notificationId(packageName), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val name = label(context, packageName)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time is up for $name")
            .setContentText("Your allowed usage time for this app has ended. Ask the authorized parent to verify to continue.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your allowed usage time for this app has ended. Ask the authorized parent to verify to continue."),
            )
            .setContentIntent(content)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            compat.notify(notificationId(packageName), notification)
        } catch (e: SecurityException) {
            // Notification permission was withdrawn; nothing else to do.
        }
    }

    fun cancel(context: Context, packageName: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(packageName))
    }
}
