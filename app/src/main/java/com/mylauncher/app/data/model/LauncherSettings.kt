package com.mylauncher.app.data.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
) {
    companion object {
        const val MIN_COLUMNS = 4
        const val MAX_COLUMNS = 6
        const val DEFAULT_COLUMNS = 4
    }
}
