package com.mylauncher.app

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.launcher.search.SearchResult
import com.mylauncher.app.launcher.search.SmartSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSearchTest {
    private fun app(label: String, pkg: String = "com.example.${label.lowercase().replace(" ", "")}") =
        AppInfo(pkg, "$pkg.Main", label)

    private val youtube = app("YouTube", "com.google.android.youtube")
    private val music = app("YouTube Music", "com.google.android.apps.youtube.music")
    private val yt2 = app("My YouTube Notes")
    private val chrome = app("Chrome", "com.android.chrome")
    private val chat = app("Chatter")
    private val apps = listOf(chat, chrome, music, yt2, youtube)

    private fun labels(r: List<SearchResult>) = r.filterIsInstance<SearchResult.AppResult>().map { it.app.label }

    @Test fun exactNameComesFirst() {
        assertEquals("YouTube", labels(SmartSearch.search("youtube", apps)).first())
    }

    @Test fun prefixBeforeWordPrefixBeforeContains() {
        // "you": prefix matches (YouTube, YouTube Music) before the word-prefix match (My YouTube Notes).
        assertEquals(listOf("YouTube", "YouTube Music", "My YouTube Notes"), labels(SmartSearch.search("you", apps)))
        // "tube" only appears inside words, so all three are partial matches, ordered by label.
        assertEquals(listOf("My YouTube Notes", "YouTube", "YouTube Music"), labels(SmartSearch.search("tube", apps)))
    }

    @Test fun frequentThenRecentBreakTies() {
        val a = app("Alpha One"); val b = app("Alpha Two"); val c = app("Alpha Three")
        val stats = mapOf(
            b.packageName to LaunchStat(5, 100L),
            c.packageName to LaunchStat(5, 200L),
            a.packageName to LaunchStat(1, 900L),
        )
        assertEquals(listOf("Alpha Three", "Alpha Two", "Alpha One"), labels(SmartSearch.search("alpha", listOf(a, b, c), stats = stats)))
    }

    @Test fun wifiFindsSettingShortcuts() {
        val r = SmartSearch.search("wifi", apps).filterIsInstance<SearchResult.SettingResult>().map { it.shortcut.id }
        assertTrue("wifi" in r)
        assertTrue("hotspot" in r)
    }

    @Test fun internetFindsInternetSettings() {
        val r = SmartSearch.search("internet", apps).filterIsInstance<SearchResult.SettingResult>().map { it.shortcut.id }
        assertTrue("internet" in r)
    }

    @Test fun singleLetterDoesNotFloodShortcuts() {
        val r = SmartSearch.search("s", apps)
        assertTrue(r.none { it is SearchResult.SettingResult || it is SearchResult.ActionResult })
    }

    @Test fun folderNameListsItsApps() {
        val folder = Folder("f1", "Work stuff", listOf(chat.packageName, chrome.packageName))
        val r = SmartSearch.search("work", apps, folders = listOf(folder)).filterIsInstance<SearchResult.AppResult>()
        assertEquals(setOf("Chatter", "Chrome"), r.map { it.app.label }.toSet())
        assertTrue(r.all { it.inFolder == "Work stuff" })
    }

    @Test fun webFallbackOnlyWhenNoUsefulApp() {
        assertTrue(SmartSearch.search("zzqx", apps).any { it is SearchResult.WebResult })
        assertTrue(SmartSearch.search("chrome", apps).none { it is SearchResult.WebResult })
        assertTrue(SmartSearch.search("zzqx", apps, includeWebFallback = false).none { it is SearchResult.WebResult })
    }

    @Test fun blankQueryReturnsAllApps() {
        assertEquals(apps.size, SmartSearch.search("  ", apps).size)
    }

    @Test fun systemActionsAreFound() {
        val r = SmartSearch.search("backup", apps).filterIsInstance<SearchResult.ActionResult>()
        assertTrue(r.isNotEmpty())
    }
}
