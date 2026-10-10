package com.mylauncher.app.data.model

/** One app launch from this launcher. Stored on this device only. */
data class LaunchEvent(val packageName: String, val timeMs: Long)
