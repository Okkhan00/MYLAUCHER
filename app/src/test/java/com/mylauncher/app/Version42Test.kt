package com.mylauncher.app

import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryCodec
import com.mylauncher.app.categories.CategoryConfig
import com.mylauncher.app.categories.CategoryOrganizer
import com.mylauncher.app.categories.CategoryPreview
import com.mylauncher.app.categories.CategoryVisuals
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.SmartSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Version42Test {
    private fun app(label: String, pkg: String) = AppInfo(pkg, "$pkg.Main", label)

    @Test fun everyCategoryHasItsOwnIcon() {
        val icons = AppCategory.entries.map { CategoryVisuals.emoji(it) }
        assertTrue(icons.all { it.isNotBlank() })
        assertEquals(icons.size, icons.toSet().size)
    }

    @Test fun categoryNamesMatchTheDesign() {
        assertEquals("Entertainment", AppCategory.MEDIA.defaultName)
        assertEquals("Productivity", AppCategory.WORK.defaultName)
        assertEquals("Other Apps", AppCategory.OTHER.defaultName)
        for (n in listOf("Communication", "Social", "Photography", "Tools", "Shopping", "Travel", "Games", "Finance")) {
            assertTrue(n, AppCategory.entries.any { it.defaultName == n })
        }
    }

    @Test fun savedCategoryConfigFromEarlierVersionKeepsIdsAndCustomNames() {
        // Ids are what is saved, so renaming the default labels never touches saved data.
        val saved = "media\tMy Videos\t0\nwork\t\t1\nother\t\t0"
        val config = CategoryCodec.decodeConfig(saved)
        assertEquals("My Videos", config.entryFor(AppCategory.MEDIA).displayName)
        assertEquals("Productivity", config.entryFor(AppCategory.WORK).displayName)
        assertTrue(config.entryFor(AppCategory.WORK).hidden)
    }

    @Test fun previewCountAdaptsToCardWidth() {
        assertEquals(4, CategoryPreview.count(126f, 10))
        assertEquals(3, CategoryPreview.count(106f, 10))
        assertEquals(1, CategoryPreview.count(10f, 10))
        assertEquals(4, CategoryPreview.count(500f, 10))
        assertEquals(2, CategoryPreview.count(500f, 2))
        assertEquals(0, CategoryPreview.count(500f, 0))
    }

    @Test fun cardCountsAndPreviewsComeFromTheSameGroup() {
        val visible = listOf(
            app("WhatsApp", "com.whatsapp"),
            app("Telegram", "org.telegram.messenger"),
            app("Camera", "com.android.camera"),
            app("Zed", "org.zed"),
        )
        val groups = CategoryOrganizer.group(visible, emptyMap(), CategoryConfig())
        val comm = groups.first { it.entry.category == AppCategory.COMMUNICATION }
        assertEquals(2, comm.apps.size)
        assertEquals(comm.apps.take(CategoryPreview.count(500f, comm.apps.size)), comm.apps)
    }

    @Test fun hiddenAppsNeverAppearBecauseTheDrawerOnlyGroupsVisibleApps() {
        val all = listOf(app("WhatsApp", "com.whatsapp"), app("Telegram", "org.telegram.messenger"))
        val hidden = setOf("org.telegram.messenger")
        val visible = all.filterNot { it.packageName in hidden }
        val groups = CategoryOrganizer.group(visible, emptyMap(), CategoryConfig())
        assertTrue(groups.flatMap { it.apps }.none { it.packageName in hidden })
    }

    @Test fun manualCategoryChoiceWinsOverAutomaticAndSurvivesSaveAndLoad() {
        val whatsapp = app("WhatsApp", "com.whatsapp")
        assertEquals(AppCategory.COMMUNICATION, CategoryOrganizer.categoryOf(whatsapp, emptyMap()))
        val overrides = mapOf("com.whatsapp" to AppCategory.WORK)
        val reloaded = CategoryCodec.decodeOverrides(CategoryCodec.encodeOverrides(overrides))
        assertEquals(AppCategory.WORK, CategoryOrganizer.categoryOf(whatsapp, reloaded))
        val groups = CategoryOrganizer.group(listOf(whatsapp), reloaded, CategoryConfig())
        assertEquals(AppCategory.WORK, groups.single().entry.category)
    }

    @Test fun drawerOpensInCategoriesByDefault() {
        assertTrue(SmartSettings().groupDrawerByCategory)
    }
}
