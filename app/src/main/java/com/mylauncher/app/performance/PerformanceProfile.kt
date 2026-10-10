package com.mylauncher.app.performance

enum class PerformanceMode(val label: String, val description: String) {
    BALANCED("Balanced", "Normal behavior."),
    BATTERY_SAVER("Battery saver", "Fewer animations and visual effects, fewer refreshes."),
    SMOOTH("Smooth", "Keeps animations on and keeps more icons cached for faster scrolling."),
}

/** What a [PerformanceMode] changes. No background work is ever started by any mode. */
data class PerformanceProfile(
    val allowAnimations: Boolean,
    /** Wallpaper blur and other optional visual effects. */
    val allowEffects: Boolean,
    val iconCacheSize: Int,
    /** How long to wait for package-change broadcasts to settle before rescanning apps. */
    val refreshDebounceMs: Long,
) {
    companion object {
        fun of(mode: PerformanceMode): PerformanceProfile = when (mode) {
            PerformanceMode.BALANCED -> PerformanceProfile(true, true, 300, 300L)
            PerformanceMode.BATTERY_SAVER -> PerformanceProfile(false, false, 150, 1_000L)
            PerformanceMode.SMOOTH -> PerformanceProfile(true, true, 600, 300L)
        }
    }
}
