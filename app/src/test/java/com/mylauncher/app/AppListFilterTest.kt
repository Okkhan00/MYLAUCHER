package com.mylauncher.app

import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.launcher.apps.AppListFilter
import com.mylauncher.app.launcher.apps.HomeEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListFilterTest {
    private fun app(pkg: String) = AppInfo(pkg, "$pkg.Main", pkg.uppercase())
    private val apps = listOf(app("a"), app("b"), app("c"))

    @Test fun hiddenAppsAreRemovedFromVisibleList() {
        assertEquals(listOf("a", "c"), AppListFilter.visible(apps, setOf("b")).map { it.packageName })
    }

    @Test fun emptyHiddenSetKeepsEverything() {
        assertEquals(apps, AppListFilter.visible(apps, emptySet()))
    }

    @Test fun homeEntriesKeepSavedOrderAndMixFoldersWithApps() {
        val folders = listOf(Folder("f1", "Work", listOf("a", "b")))
        val entries = HomeEntry.resolve(apps, listOf("c", "folder:f1"), emptySet(), folders)
        assertEquals(listOf("c", "folder:f1"), entries.map { it.key })
        val folder = entries[1] as HomeEntry.FolderEntry
        assertEquals(listOf("a", "b"), folder.apps.map { it.packageName })
    }

    @Test fun homeEntriesSkipUninstalledHiddenAndMissingFolders() {
        val folders = listOf(Folder("f1", "Work", listOf("a", "b", "gone")))
        val entries = HomeEntry.resolve(apps, listOf("gone", "c", "folder:f1", "folder:nope"), setOf("c", "b"), folders)
        assertEquals(listOf("folder:f1"), entries.map { it.key })
        assertEquals(listOf("a"), (entries[0] as HomeEntry.FolderEntry).apps.map { it.packageName })
    }

    @Test fun emptyFolderStillShowsOnHome() {
        val entries = HomeEntry.resolve(apps, listOf("folder:f1"), emptySet(), listOf(Folder("f1", "Empty", emptyList())))
        assertEquals(1, entries.size)
        assertTrue((entries[0] as HomeEntry.FolderEntry).apps.isEmpty())
    }
}
