package com.mylauncher.app

import com.mylauncher.app.data.AppListCodec
import com.mylauncher.app.data.AppListOps
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.launcher.search.SmartSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListAndIndexTest {
    private fun app(label: String, pkg: String) = AppInfo(pkg, "$pkg.Main", label)

    private val apps = listOf(
        app("Calendar", "com.google.android.calendar"),
        app("Chrome", "com.android.chrome"),
        app("My Notes", "com.example.notes"),
        app("YouTube Music", "com.google.android.apps.youtube.music"),
        app("Sound-Recorder", "com.example.rec"),
        app("Zed", "org.zed"),
    )

    @Test fun indexAgreesWithTierForManyQueries() {
        val index = AppSearch.Index(apps)
        for (q in listOf("c", "ca", "chrome", "notes", "music", "recorder", "google", "zz", "my", "YOU", "example")) {
            val viaIndex = index.match(q).associate { it.first.packageName to it.second }
            val viaTier = apps.mapNotNull { a -> AppSearch.tier(a, q)?.let { a.packageName to it } }.toMap()
            assertEquals("query=$q", viaTier, viaIndex)
        }
    }

    @Test fun smartSearchResultsIdenticalWithAndWithoutIndex() {
        val index = AppSearch.Index(apps)
        for (q in listOf("c", "chr", "music", "wifi", "nothing-here")) {
            assertEquals(SmartSearch.search(q, apps), SmartSearch.search(q, apps, index = index))
        }
    }

    @Test fun indexForADifferentListIsIgnored() {
        val other = AppSearch.Index(listOf(app("Only", "p.only")))
        // The index belongs to another list, so it must not leak its apps into these results.
        val r = SmartSearch.search("chrome", apps, index = other)
        assertTrue(r.toString().contains("Chrome"))
    }

    @Test fun replacePackageReturnsSameInstanceWhenNothingChanged() {
        val sorted = AppListOps.normalize(apps)
        val same = AppListOps.replacePackage(sorted, "com.android.chrome", listOf(app("Chrome", "com.android.chrome")))
        assertSame(sorted, same)
    }

    @Test fun replacePackageHandlesInstallUpdateAndRemoval() {
        val sorted = AppListOps.normalize(apps)
        val installed = AppListOps.replacePackage(sorted, "new.app", listOf(app("Aardvark", "new.app")))
        assertEquals("Aardvark", installed.first().label)
        val renamed = AppListOps.replacePackage(installed, "org.zed", listOf(app("Alpha Zed", "org.zed")))
        assertEquals(installed.size, renamed.size)
        assertTrue(renamed.any { it.label == "Alpha Zed" })
        val removed = AppListOps.replacePackage(renamed, "new.app", emptyList())
        assertTrue(removed.none { it.packageName == "new.app" })
    }

    @Test fun normalizeSortsAndKeepsOneEntryPerPackage() {
        val dup = apps + app("Chrome Beta Launcher", "com.android.chrome")
        val n = AppListOps.normalize(dup)
        assertEquals(1, n.count { it.packageName == "com.android.chrome" })
        assertEquals(n.map { it.label.lowercase() }, n.map { it.label.lowercase() }.sorted())
    }

    @Test fun snapshotRoundTripAndDamagedFile() {
        val list = AppListOps.normalize(apps)
        assertEquals(list, AppListCodec.decode(AppListCodec.encode(list)))
        assertTrue(AppListCodec.decode("garbage").isEmpty())
        assertTrue(AppListCodec.decode(null).isEmpty())
        val damaged = AppListCodec.encode(list).replaceFirst('\t', ' ')
        assertTrue(AppListCodec.decode(damaged).isEmpty())
    }
}
