package com.mylauncher.app.device

import java.util.Locale

/** Pure formatting helpers for the Device Dashboard. */
object DeviceFormat {
    /** 1536 -> "1.5 KB", using 1024-based units like Android's storage screens. */
    fun bytes(value: Long): String {
        if (value < 0) return "unknown"
        if (value < 1024) return "$value B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var v = value.toDouble()
        var i = -1
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024
            i++
        }
        return String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
    }

    /** Percent of [part] in [whole], 0..100; null when it cannot be computed. */
    fun percent(part: Long, whole: Long): Int? =
        if (whole <= 0 || part < 0) null else ((part * 100) / whole).toInt().coerceIn(0, 100)
}

data class StorageInfo(val totalBytes: Long, val availableBytes: Long) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)
}
