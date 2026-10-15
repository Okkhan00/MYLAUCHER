package com.mylauncher.app.data.model

import com.mylauncher.app.performance.PerformanceMode

/**
 * SYSTEM, LIGHT and DARK behave exactly as in earlier versions. DYNAMIC follows the system
 * light/dark setting and always prefers wallpaper colors (Android 12+). CUSTOM follows the system
 * light/dark setting and builds its colors from the chosen accent color.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK, DYNAMIC, CUSTOM }

enum class IconSize(val sizeDp: Int) { SMALL(44), MEDIUM(52), LARGE(60) }

enum class ClockStyle { DIGITAL, LARGE, MINIMAL }

enum class DrawerLayout { GRID, LIST }

data class LauncherSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val iconSize: IconSize = IconSize.MEDIUM,
    val columns: Int = DEFAULT_COLUMNS,
    val showLabels: Boolean = true,
    val animations: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.DIGITAL,
    val showDate: Boolean = true,
    val showSearchBar: Boolean = true,
    val drawerLayout: DrawerLayout = DrawerLayout.GRID,
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.OPEN_SEARCH,
    val doubleTap: GestureAction = GestureAction.OPEN_SEARCH,
    val showBattery: Boolean = false,
    val trackLaunches: Boolean = true,
    val onboardingDone: Boolean = false,
    val performance: PerformanceMode = PerformanceMode.BALANCED,
    // ---- V4.6: the home screen has its own grid, icon size and names. `columns`, `iconSize` and
    // `showLabels` above now describe the app drawer (and everything that is not the home screen). ----
    val homeColumns: Int = DEFAULT_COLUMNS,
    val homeIconSize: IconSize = IconSize.MEDIUM,
    val homeShowLabels: Boolean = true,
    /** Number of dock slots; 0 means no dock (the default, so nothing changes for existing users). */
    val dockCount: Int = 0,
) {
    companion object {
        const val MIN_COLUMNS = 4
        const val MAX_COLUMNS = 6
        const val DEFAULT_COLUMNS = 4
        const val MAX_DOCK = 5

        /**
         * Home values did not exist before V4.6. When they were never saved the home screen keeps the
         * value the user already had, so updating changes nothing visible.
         */
        fun resolveColumns(home: Int?, legacy: Int?): Int =
            (home ?: legacy ?: DEFAULT_COLUMNS).coerceIn(MIN_COLUMNS, MAX_COLUMNS)
    }
}
