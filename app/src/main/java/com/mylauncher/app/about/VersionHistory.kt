package com.mylauncher.app.about

/** One release in the in-app version history. Only features that were genuinely implemented are listed. */
data class ReleaseNote(val version: String, val title: String, val points: List<String>)

object VersionHistory {
    val releases: List<ReleaseNote> = listOf(
        ReleaseNote(
            "4.5.1", "Uninstall fix",
            listOf(
                "Fixed uninstalling apps: Android now accepts the request and shows its confirmation screen",
                "Clear messages for apps that can't be uninstalled here",
            ),
        ),
        ReleaseNote(
            "4.5", "Select several apps",
            listOf(
                "Select several apps and change their category, add to favorites, hide, lock, move to a folder or uninstall them together",
                "Create your own categories from a selection",
                "Categories now show one at a time, with a position indicator",
                "Settings keeps its scroll position and is grouped into clearer sections",
            ),
        ),
        ReleaseNote(
            "4.4", "Reliability and polish",
            listOf(
                "More reliable app list: new, updated and removed apps are picked up, also when you return to the launcher",
                "Swipe left or right inside an open category to move to the next or previous one",
                "About section with version history",
                "Switches for showing Recently used and Suggested apps",
            ),
        ),
        ReleaseNote(
            "4.3", "Themes and app usage timer",
            listOf(
                "Theme gallery with built-in themes and your own custom theme",
                "App usage timer: daily limits for chosen apps, unlocked with a separate parent PIN",
            ),
        ),
        ReleaseNote(
            "4.2", "Cleaner drawer and settings",
            listOf(
                "App drawer with Categories and All Apps tabs and category cards",
                "Settings regrouped into clear sections; short welcome guide on first start",
            ),
        ),
        ReleaseNote(
            "4.1", "Performance and stability",
            listOf(
                "Faster search, smoother scrolling and less work on the main thread",
            ),
        ),
        ReleaseNote(
            "3 and earlier", "Foundation",
            listOf(
                "Home screen, app drawer, search, favorites, folders, hidden apps and gestures",
                "Launcher lock, app lock, categories, suggestions, wallpaper options, widgets, backup and restore",
            ),
        ),
    )
}
