package com.mylauncher.app.launcher.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryInfo(val percent: Int, val charging: Boolean)

/** Reads the sticky battery broadcast. Needs no permission and registers no receiver. */
object BatteryReader {
    fun read(context: Context): BatteryInfo? {
        val intent: Intent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            null
        } ?: return null
        val percent = BatteryMath.percentOf(
            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
            intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1),
        ) ?: return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryInfo(percent, charging)
    }
}
