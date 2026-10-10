package com.mylauncher.app

import com.mylauncher.app.categories.AppCategorizer
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryCodec
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.categories.CategoryOrganizer
import com.mylauncher.app.data.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesTest {
    private fun cat(pkg: String, label: String, sys: Int = -1) = AppCategorizer.categorize(pkg, label, sys)

    @Test fun commonAppsAreCategorizedFromPackageNames() {
        assertEquals(AppCategory.COMMUNICATION, cat("com.whatsapp", "WhatsApp"))
        assertEquals(AppCategory.COMMUNICATION, cat("com.google.android.gm", "Gmail"))
        assertEquals(AppCategory.SOCIAL, cat("com.instagram.android", "Instagram"))
        assertEquals(AppCategory.MEDIA, cat("com.google.android.youtube", "YouTube"))
        assertEquals(AppCategory.MEDIA, cat("com.spotify.music", "Spotify"))
        assertEquals(AppCategory.TRAVEL, cat("com.google.android.apps.maps", "Maps"))
        assertEquals(AppCategory.PHOTOGRAPHY, cat("com.android.camera2", "Camera"))
        assertEquals(AppCategory.PHOTOGRAPHY, cat("com.google.android.apps.photos", "Photos"))
        assertEquals(AppCategory.TOOLS, cat("com.android.settings", "Settings"))
        assertEquals(AppCategory.TOOLS, cat("com.android.chrome", "Chrome"))
        assertEquals(AppCategory.FINANCE, cat("com.paypal.android.p2pmobile", "PayPal"))
        assertEquals(AppCategory.WORK, cat("com.google.android.apps.docs", "Docs"))
    }

    @Test fun systemCategoryIsAUsefulHint() {
        assertEquals(AppCategory.GAMES, cat("com.studio.xyz", "Blobs", sys = 0))
        assertEquals(AppCategory.MEDIA, cat("com.studio.xyz", "Blobs", sys = 2))
        assertEquals(AppCategory.OTHER, cat("com.studio.xyz", "Blobs"))
    }

    @Test fun shortKeywordsDoNotMatchInsideWords() {
        // "tv" and "gm" must be whole tokens: "gmx" / "outvoice" must not match.
        assertEquals(AppCategory.OTHER, cat("com.example.outvoice", "Outvoice"))
    }

    @Test fun manualOverrideBeatsHeuristic() {
        val app = AppInfo("com.whatsapp", "Main", "WhatsApp")
        assertEquals(AppCategory.COMMUNICATION, CategoryOrganizer.categoryOf(app, emptyMap()))
        assertEquals(AppCategory.WORK, CategoryOrganizer.categoryOf(app, mapOf("com.whatsapp" to AppCategory.WORK)))
    }

    @Test fun groupingFollowsOrderHidesAndDropsEmpty() {
        val apps = listOf(
            AppInfo("com.whatsapp", "M", "WhatsApp"),
            AppInfo("com.instagram.android", "M", "Instagram"),
            AppInfo("com.android.settings", "M", "Settings"),
        )
        var config = CategoryConfig().move(AppCategory.TOOLS, -100)
        var groups = CategoryOrganizer.group(apps, emptyMap(), config)
        assertEquals(listOf(AppCategory.TOOLS, AppCategory.SOCIAL, AppCategory.COMMUNICATION), groups.map { it.entry.category })

        config = config.setHidden(AppCategory.SOCIAL, true)
        groups = CategoryOrganizer.group(apps, emptyMap(), config)
        assertEquals(listOf(AppCategory.TOOLS, AppCategory.COMMUNICATION), groups.map { it.entry.category })
        assertEquals(3, CategoryOrganizer.group(apps, emptyMap(), config, includeHidden = true).size)

        config = config.rename(AppCategory.TOOLS, "  Utilities ")
        assertEquals("Utilities", CategoryOrganizer.group(apps, emptyMap(), config).first().entry.displayName)
    }

    @Test fun configRoundTripsAndHealsBadData() {
        val config = CategoryConfig().rename(AppCategory.GAMES, "Fun").setHidden(AppCategory.WORK, true).move(AppCategory.OTHER, -3)
        assertEquals(config.normalized(), CategoryCodec.decodeConfig(CategoryCodec.encodeConfig(config)))
        val healed = CategoryCodec.decodeConfig("garbage\nsocial\t\t1\nunknown\tx\t0")
        assertEquals(AppCategory.entries.size, healed.entries.size)
        assertEquals(AppCategory.SOCIAL, healed.entries.first().category)
        assertTrue(healed.entries.first().hidden)
        assertEquals(CategoryConfig().entries, CategoryCodec.decodeConfig(null).entries)
    }

    @Test fun overridesRoundTripAndSkipInvalidLines() {
        val map = mapOf("a.b" to AppCategory.GAMES, "c.d" to AppCategory.WORK)
        assertEquals(map, CategoryCodec.decodeOverrides(CategoryCodec.encodeOverrides(map)))
        assertEquals(mapOf("a.b" to AppCategory.GAMES), CategoryCodec.decodeOverrides("a.b\tgames\nbad\nx\tnope"))
        assertNull(CategoryCodec.sanitizeName("   "))
        assertEquals(CategoryCodec.MAX_NAME, CategoryCodec.sanitizeName("x".repeat(80))!!.length)
    }
}
