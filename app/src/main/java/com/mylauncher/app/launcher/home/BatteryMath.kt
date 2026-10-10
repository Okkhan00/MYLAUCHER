package com.mylauncher.app.launcher.home

object BatteryMath {
    /** Returns 0..100, or null when the system reported unusable values. */
    fun percentOf(level: Int, scale: Int): Int? =
        if (level < 0 || scale <= 0) null else (level * 100 / scale).coerceIn(0, 100)
}
