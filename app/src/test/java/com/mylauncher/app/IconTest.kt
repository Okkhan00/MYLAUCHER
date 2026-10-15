package com.mylauncher.app

import com.mylauncher.app.backup.MiniJson
import com.mylauncher.app.icons.AppFilter
import com.mylauncher.app.icons.CustomIconCodec
import com.mylauncher.app.icons.CustomIconDef
import com.mylauncher.app.icons.CustomShape
import com.mylauncher.app.icons.IconExport
import com.mylauncher.app.icons.IconPackMapping
import com.mylauncher.app.icons.IconPrecedence
import com.mylauncher.app.icons.IconSourceKind
import com.mylauncher.app.icons.IconStudioMath
import com.mylauncher.app.settings.SettingsSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IconTest {

    @Test
    fun precedenceIsCustomThenPackThenOriginal() {
        assertEquals(IconSourceKind.CUSTOM, IconPrecedence.resolve(hasCustom = true, hasPackIcon = true))
        assertEquals(IconSourceKind.CUSTOM, IconPrecedence.resolve(hasCustom = true, hasPackIcon = false))
        assertEquals(IconSourceKind.PACK, IconPrecedence.resolve(hasCustom = false, hasPackIcon = true))
        assertEquals(IconSourceKind.ORIGINAL, IconPrecedence.resolve(hasCustom = false, hasPackIcon = false))
    }

    @Test
    fun componentKeyParsing() {
        assertEquals("com.a/com.a.Main", AppFilter.componentKey("ComponentInfo{com.a/com.a.Main}"))
        assertEquals("com.a/com.a.Main", AppFilter.componentKey("  ComponentInfo{com.a/.Main}  "))
        assertNull(AppFilter.componentKey(null))
        assertNull(AppFilter.componentKey(""))
        assertNull(AppFilter.componentKey("com.a/com.a.Main"))
        assertNull(AppFilter.componentKey("ComponentInfo{}"))
        assertNull(AppFilter.componentKey("ComponentInfo{com.a/}"))
        assertNull(AppFilter.componentKey("ComponentInfo{/Main}"))
        assertNull(AppFilter.componentKey("ComponentInfo{com a/Main}"))
    }

    @Test
    fun drawableNamesAreValidated() {
        assertTrue(AppFilter.isValidDrawableName("ic_chrome_2"))
        assertFalse(AppFilter.isValidDrawableName(""))
        assertFalse(AppFilter.isValidDrawableName(null))
        assertFalse(AppFilter.isValidDrawableName("../etc/passwd"))
        assertFalse(AppFilter.isValidDrawableName("a".repeat(101)))
    }

    @Test
    fun mappingFindsExactComponentThenPackageFallback() {
        val m = IconPackMapping(
            mapOf(
                "com.a/com.a.Main" to "icon_a",
                "com.b/com.b.One" to "icon_b1",
                "com.b/com.b.Two" to "icon_b2",
            ),
        )
        assertEquals(3, m.size)
        assertEquals("icon_a", m.drawableFor("com.a", "com.a.Main"))
        assertEquals("icon_b2", m.drawableFor("com.b", "com.b.Two"))
        assertEquals("icon_b1", m.drawableFor("com.b", "com.b.Other")) // package fallback: first mapping
        assertNull(m.drawableFor("com.zzz", "com.zzz.Main"))
        assertNull(IconPackMapping(emptyMap()).drawableFor("a", "b"))
    }

    @Test
    fun customIconCodecRoundTripsAndSkipsBadLines() {
        val defs = mapOf(
            "com.a" to CustomIconDef(CustomShape.CIRCLE, 0xFF112233.toInt(), 10, 3),
            "com.b" to CustomIconDef(),
        )
        assertEquals(defs, CustomIconCodec.decode(CustomIconCodec.encode(defs)))
        val dirty = "com.a\tCIRCLE\t1\t5\t1\ngarbage\ncom.b\tTRIANGLE\t1\t5\t1\ncom.c\tSQUARE\tx\t5\t1\n\tSQUARE\t1\t1\t1\ncom.d\tSQUARE\t1\t99\t0"
        val out = CustomIconCodec.decode(dirty)
        assertEquals(setOf("com.a", "com.d"), out.keys)
        assertEquals(CustomIconDef.MAX_PADDING, out.getValue("com.d").paddingPercent) // clamped
        assertEquals(1, out.getValue("com.d").version) // never below 1
        assertTrue(CustomIconCodec.decode(null).isEmpty())
        assertTrue(CustomIconCodec.decode("").isEmpty())
    }

    @Test
    fun cropRectStaysInsideThePicture() {
        // Wide picture, no zoom: the largest square, centered.
        assertEquals(listOf(300, 0, 400), IconStudioMath.cropRect(1000, 400, 1f, 0f, 0f).toList())
        // Pan to both ends.
        assertEquals(0, IconStudioMath.cropRect(1000, 400, 1f, -1f, 0f)[0])
        assertEquals(600, IconStudioMath.cropRect(1000, 400, 1f, 1f, 0f)[0])
        // Zoom 2 halves the side.
        assertEquals(200, IconStudioMath.cropRect(1000, 400, 2f, 0f, 0f)[2])
        for (zoom in listOf(0.2f, 1f, 3.3f, 99f)) for (pan in listOf(-5f, -1f, 0f, 0.4f, 1f, 7f)) {
            val (l, t, s) = IconStudioMath.cropRect(640, 480, zoom, pan, -pan).toList()
            assertTrue(l >= 0 && t >= 0 && s >= 1)
            assertTrue(l + s <= 640 && t + s <= 480)
        }
    }

    @Test
    fun sampleSizeKeepsLargePicturesSmall() {
        assertEquals(1, IconStudioMath.sampleSize(1000, 800))
        assertEquals(1, IconStudioMath.sampleSize(2048, 100))
        assertEquals(2, IconStudioMath.sampleSize(4096, 3000))
        assertEquals(4, IconStudioMath.sampleSize(16000, 9000))
        assertTrue(16000 / IconStudioMath.sampleSize(16000, 9000) <= 2 * IconStudioMath.MAX_SOURCE_SIDE)
    }

    @Test
    fun validationRejectsBadPictures() {
        assertNull(IconStudioMath.validate(500_000, 800, 600))
        assertNotNull(IconStudioMath.validate(500_000, 0, 600))
        assertNotNull(IconStudioMath.validate(IconStudioMath.MAX_FILE_BYTES + 1, 800, 600))
        assertNotNull(IconStudioMath.validate(1000, 20, 900))
    }

    @Test
    fun paddingIsClamped() {
        assertEquals(0, IconStudioMath.paddingPx(100, 0))
        assertEquals(10, IconStudioMath.paddingPx(100, 10))
        assertEquals(30, IconStudioMath.paddingPx(100, 500))
        assertEquals(0, IconStudioMath.paddingPx(100, -4))
    }

    @Test
    fun exportManifestIsVersionedAndSafe() {
        assertEquals("com.example.app.png", IconExport.safeFileName("com.example.app"))
        assertEquals("a_b_c.png", IconExport.safeFileName("a/b\\c"))
        assertEquals("icon.png", IconExport.safeFileName("..."))
        assertTrue(IconExport.safeFileName("../../evil").startsWith("_"))
        assertFalse(IconExport.safeFileName("../../evil").contains("/"))
        val json = IconExport.manifest(mapOf("com.b" to CustomIconDef(), "com.a" to CustomIconDef(CustomShape.CIRCLE, 5, 7, 2)), 123L)
        @Suppress("UNCHECKED_CAST")
        val parsed = MiniJson.parse(json) as Map<String, Any?>
        assertEquals(IconExport.FORMAT, parsed["format"])
        assertEquals(1.0, parsed["version"])
        assertTrue((parsed["note"] as String).contains("not an installable"))
        @Suppress("UNCHECKED_CAST")
        val icons = parsed["icons"] as List<Map<String, Any?>>
        assertEquals(listOf("com.a", "com.b"), icons.map { it["package"] })
        assertEquals("icons/com.a.png", icons[0]["file"])
    }

    @Test
    fun settingsSectionsAreRestoredByNameAndFallBackToDashboard() {
        SettingsSection.entries.forEach { assertEquals(it, SettingsSection.fromName(it.name)) }
        assertNull(SettingsSection.fromName(null))
        assertNull(SettingsSection.fromName("REMOVED_SECTION"))
        assertEquals(SettingsSection.entries.size, SettingsSection.entries.map { it.title }.toSet().size)
    }
}
